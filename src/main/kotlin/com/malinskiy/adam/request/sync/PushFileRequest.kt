/*
 * Copyright (C) 2019 Anton Malinskiy
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

package com.malinskiy.adam.request.sync

import com.malinskiy.adam.Const
import com.malinskiy.adam.extension.toByteArray
import com.malinskiy.adam.request.async.AsyncChannelRequest
import com.malinskiy.adam.transport.AndroidReadChannel
import com.malinskiy.adam.transport.AndroidWriteChannel
import io.ktor.util.cio.readChannel
import kotlinx.coroutines.Dispatchers
import java.io.File
import kotlin.coroutines.CoroutineContext

class PushFileRequest(
    val local: File,
    val remotePath: String,
    val mode: String = "0644",
    coroutineContext: CoroutineContext = Dispatchers.IO
) : AsyncChannelRequest<Double>() {

    //Not sure yet when to properly close this
    val channel = local.readChannel(coroutineContext = coroutineContext)
    val buffer = ByteArray(8 + Const.MAX_FILE_PACKET_LENGTH)
    val totalBytes = local.length()
    var currentPosition = 0L

    override suspend fun handshake(readChannel: AndroidReadChannel, writeChannel: AndroidWriteChannel) {
        super.handshake(readChannel, writeChannel)

        val type = Const.Message.SEND

        val mode = (',' + (mode.toInt(8) and "0777".toInt(8)).toString()).toByteArray(Const.DEFAULT_TRANSPORT_ENCODING)
        val path = remotePath.toByteArray(Const.DEFAULT_TRANSPORT_ENCODING)
        val packetLength = (path.size + mode.size)
        val size = packetLength.toByteArray().reversedArray()

        val cmd = ByteArray(8 + path.size + 4)

        type.copyInto(cmd)
        size.copyInto(cmd, 4)
        path.copyInto(cmd, 8)
        mode.copyInto(cmd, 8 + path.size)

        writeChannel.write(cmd)
    }

    override suspend fun readElement(readChannel: AndroidReadChannel, writeChannel: AndroidWriteChannel): Double {
        val available = channel.readAvailable(buffer, 8, Const.MAX_FILE_PACKET_LENGTH)
        return when {
            available < 0 -> {
                channel.cancel(null)
                Const.Message.DONE.copyInto(buffer)
                (local.lastModified() / 1000).toInt().toByteArray().copyInto(buffer, destinationOffset = 4)
                writeChannel.write(request = buffer, length = 8)
                readChannel.cancel(null)
                writeChannel.close(null)
                1.0
            }
            available > 0 -> {
                Const.Message.DATA.copyInto(buffer)
                available.toByteArray().reversedArray().copyInto(buffer, destinationOffset = 4)
                writeChannel.writeFully(buffer, 0, available + 8)
                currentPosition += available
                currentPosition.toDouble() / totalBytes
            }
            else -> currentPosition.toDouble() / totalBytes
        }
    }

    override fun serialize() = createBaseRequest("sync:")

    override fun validate(): Boolean {
        val bytes = remotePath.toByteArray(Const.DEFAULT_TRANSPORT_ENCODING)
        return bytes.size <= Const.MAX_REMOTE_PATH_LENGTH
    }
}