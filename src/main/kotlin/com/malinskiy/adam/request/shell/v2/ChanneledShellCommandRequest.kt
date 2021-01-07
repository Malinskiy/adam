/*
 * Copyright (C) 2020 Anton Malinskiy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.malinskiy.adam.request.shell.v2

import com.malinskiy.adam.Const
import com.malinskiy.adam.request.AsyncChannelRequest
import com.malinskiy.adam.request.NonSpecifiedTarget
import com.malinskiy.adam.request.Target
import com.malinskiy.adam.transport.AndroidReadChannel
import com.malinskiy.adam.transport.AndroidWriteChannel
import io.ktor.utils.io.readIntLittleEndian
import io.ktor.utils.io.writeByte
import io.ktor.utils.io.writeFully
import io.ktor.utils.io.writeIntLittleEndian
import kotlinx.coroutines.channels.ReceiveChannel

open class ChanneledShellCommandRequest(
    private val cmd: String,
    channel: ReceiveChannel<ShellCommandInputChunk>,
    target: Target = NonSpecifiedTarget
) : AsyncChannelRequest<ShellCommandResultChunk, ShellCommandInputChunk>(target = target, channel = channel) {

    val data = ByteArray(Const.MAX_PACKET_LENGTH)

    override suspend fun readElement(readChannel: AndroidReadChannel, writeChannel: AndroidWriteChannel): ShellCommandResultChunk? {
        //Skip if nothing is happening
        if (readChannel.availableForRead == 0) {
            return null
        }
        return when (MessageType.of(readChannel.readByte().toInt())) {
            MessageType.STDOUT -> {
                val length = readChannel.readIntLittleEndian()
                readChannel.readFully(data, 0, length)
                ShellCommandResultChunk(stdout = String(data, 0, length, Const.DEFAULT_TRANSPORT_ENCODING))
            }
            MessageType.STDERR -> {
                val length = readChannel.readIntLittleEndian()
                readChannel.readFully(data, 0, length)
                ShellCommandResultChunk(stderr = String(data, 0, length, Const.DEFAULT_TRANSPORT_ENCODING))
            }
            MessageType.EXIT -> {
                val ignoredLength = readChannel.readIntLittleEndian()
                val exitCode = readChannel.readByte().toInt()
                ShellCommandResultChunk(exitCode = exitCode)
            }
            else -> {
                null
            }
        }
    }

    /**
     * Handles stdin
     */
    override suspend fun writeElement(element: ShellCommandInputChunk, readChannel: AndroidReadChannel, writeChannel: AndroidWriteChannel) {
        element.stdin?.let {
            writeChannel.writeByte(MessageType.STDIN.toValue())
            val bytes = it.toByteArray(Const.DEFAULT_TRANSPORT_ENCODING)
            writeChannel.writeIntLittleEndian(bytes.size)
            writeChannel.writeFully(bytes)
        }

        if (element.close) {
            writeChannel.writeByte(MessageType.CLOSE_STDIN.toValue())
            writeChannel.writeIntLittleEndian(0)
        }
    }

    override fun serialize() = createBaseRequest("shell,v2,raw:$cmd")
}

data class ShellCommandResultChunk(
    val stdout: String? = null,
    val stderr: String? = null,
    val exitCode: Int? = null
)

data class ShellCommandInputChunk(
    val stdin: String? = null,
    val close: Boolean = false
)
