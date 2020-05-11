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
import com.malinskiy.adam.exception.PullFailedException
import com.malinskiy.adam.exception.UnsupportedSyncProtocolException
import com.malinskiy.adam.extension.toByteArray
import com.malinskiy.adam.extension.toInt
import com.malinskiy.adam.request.async.AsyncChannelRequest
import com.malinskiy.adam.transport.AndroidReadChannel
import com.malinskiy.adam.transport.AndroidWriteChannel
import io.ktor.util.cio.writeChannel
import kotlinx.coroutines.Dispatchers
import java.io.File
import kotlin.coroutines.CoroutineContext

class PullFileRequest(
    val remotePath: String,
    val local: File,
    coroutineContext: CoroutineContext = Dispatchers.IO
) : AsyncChannelRequest<Double>() {

    //Not sure yet when to properly close this
    val channel = local.writeChannel(coroutineContext = coroutineContext)
    var totalBytes = 1
    var currentPosition = 0L


    override suspend fun handshake(readChannel: AndroidReadChannel, writeChannel: AndroidWriteChannel) {
        super.handshake(readChannel, writeChannel)

        totalBytes = statSize(readChannel, writeChannel)

        val type = Const.Message.RECV

        val path = remotePath.toByteArray(Const.DEFAULT_TRANSPORT_ENCODING)
        val size = path.size.toByteArray().reversedArray()

        val cmd = ByteArray(8 + path.size)

        type.copyInto(cmd)
        size.copyInto(cmd, 4)
        path.copyInto(cmd, 8)

        writeChannel.write(cmd)
    }

    private suspend fun statSize(readChannel: AndroidReadChannel, writeChannel: AndroidWriteChannel): Int {
        val bytes = ByteArray(16)

        val type = Const.Message.STAT

        val path = remotePath.toByteArray(Const.DEFAULT_TRANSPORT_ENCODING)
        val size = path.size.toByteArray().reversedArray()

        val cmd = ByteArray(8 + path.size)

        type.copyInto(cmd)
        size.copyInto(cmd, 4)
        path.copyInto(cmd, 8)

        writeChannel.write(cmd)
        readChannel.readFully(bytes, 0, 16)

        if (!bytes.copyOfRange(0, 4).contentEquals(Const.Message.STAT)) throw UnsupportedSyncProtocolException()

        return bytes.copyOfRange(8, 12).toInt()
    }

    private val headerBuffer = ByteArray(8)
    private val dataBuffer = ByteArray(Const.MAX_FILE_PACKET_LENGTH)

    override suspend fun readElement(readChannel: AndroidReadChannel, writeChannel: AndroidWriteChannel): Double {
        readChannel.readFully(headerBuffer, 0, 8)

        val header = headerBuffer.copyOfRange(0, 4)
        when {
            header.contentEquals(Const.Message.DONE) -> {
                channel.close(null)
                readChannel.cancel(null)
                writeChannel.close(null)
                return 1.0
            }
            header.contentEquals(Const.Message.DATA) -> {
                val available = headerBuffer.copyOfRange(4, 8).toInt()
                if (available > Const.MAX_FILE_PACKET_LENGTH) {
                    throw UnsupportedSyncProtocolException()
                }
                readChannel.readFully(dataBuffer, 0, available)
                channel.writeFully(dataBuffer, 0, available)

                currentPosition += available

                return currentPosition.toDouble() / totalBytes
            }
            header.contentEquals(Const.Message.FAIL) -> {
                throw PullFailedException("Failed to pull file $remotePath")
            }
            else -> {
                throw UnsupportedSyncProtocolException("Unexpected header message ${String(header, Const.DEFAULT_TRANSPORT_ENCODING)}")
            }
        }
    }

    override fun serialize() = createBaseRequest("sync:")

    override fun validate(): Boolean {
        val bytes = remotePath.toByteArray(Const.DEFAULT_TRANSPORT_ENCODING)
        return bytes.size <= Const.MAX_REMOTE_PATH_LENGTH
    }
}