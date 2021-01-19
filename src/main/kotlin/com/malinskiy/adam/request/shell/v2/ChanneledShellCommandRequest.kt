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
import com.malinskiy.adam.transport.Socket
import com.malinskiy.adam.transport.withDefaultBuffer
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel

open class ChanneledShellCommandRequest(
    private val cmd: String,
    channel: ReceiveChannel<ShellCommandInputChunk>,
    target: Target = NonSpecifiedTarget
) : AsyncChannelRequest<ShellCommandResultChunk, ShellCommandInputChunk>(target = target, channel = channel) {

    val data = ByteArray(Const.MAX_PACKET_LENGTH)

    override suspend fun readElement(socket: Socket, sendChannel: SendChannel<ShellCommandResultChunk>): Boolean {
        withDefaultBuffer {
            val readAvailable = socket.readAvailable(this.array(), 0, 1)
            when (readAvailable) {
                //Skip as if nothing is happening
                0, -1 -> return false
            }

            val readByte = this.get(0)
            when (MessageType.of(readByte.toInt())) {
                MessageType.STDOUT -> {
                    val length = socket.readIntLittleEndian()
                    socket.readFully(data, 0, length)
                    sendChannel.send(ShellCommandResultChunk(stdout = String(data, 0, length, Const.DEFAULT_TRANSPORT_ENCODING)))
                }
                MessageType.STDERR -> {
                    val length = socket.readIntLittleEndian()
                    socket.readFully(data, 0, length)
                    sendChannel.send(ShellCommandResultChunk(stderr = String(data, 0, length, Const.DEFAULT_TRANSPORT_ENCODING)))
                }
                MessageType.EXIT -> {
                    val ignoredLength = socket.readIntLittleEndian()
                    val exitCode = socket.readByte().toInt()
                    sendChannel.send(ShellCommandResultChunk(exitCode = exitCode))
                }
                else -> Unit
            }

            return false
        }
    }

    /**
     * Handles stdin
     */
    override suspend fun writeElement(element: ShellCommandInputChunk, socket: Socket) {
        element.stdin?.let {
            socket.writeByte(MessageType.STDIN.toValue())
            val bytes = it.toByteArray(Const.DEFAULT_TRANSPORT_ENCODING)
            socket.writeIntLittleEndian(bytes.size)
            socket.writeFully(bytes)
        }

        if (element.close) {
            socket.writeByte(MessageType.CLOSE_STDIN.toValue())
            socket.writeIntLittleEndian(0)
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
