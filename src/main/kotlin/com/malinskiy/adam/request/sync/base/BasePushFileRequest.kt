/*
 * Copyright (C) 2021 Anton Malinskiy
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

package com.malinskiy.adam.request.sync.base

import com.malinskiy.adam.Const
import com.malinskiy.adam.exception.PushFailedException
import com.malinskiy.adam.extension.compatLimit
import com.malinskiy.adam.extension.readTransportResponse
import com.malinskiy.adam.extension.toByteArray
import com.malinskiy.adam.io.AsyncFileReader
import com.malinskiy.adam.request.AsyncChannelRequest
import com.malinskiy.adam.request.ValidationResponse
import com.malinskiy.adam.transport.Socket
import com.malinskiy.adam.transport.withDefaultBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.SendChannel
import java.io.File
import kotlin.coroutines.CoroutineContext

abstract class BasePushFileRequest(
    private val local: File,
    protected val remotePath: String,
    protected val mode: String = "0777",
    coroutineContext: CoroutineContext = Dispatchers.IO
) : AsyncChannelRequest<Double, Unit>() {
    protected val fileReader = AsyncFileReader(
        file = local,
        start = 0,
        offset = 8,
        length = Const.MAX_FILE_PACKET_LENGTH - 8,
        coroutineContext = coroutineContext
    )
    protected var totalBytes = local.length()
    protected var currentPosition = 0L
    protected val modeValue: Int
        get() = mode.toInt(8) and "0777".toInt(8)

    override suspend fun handshake(socket: Socket) {
        super.handshake(socket)
        fileReader.start()
    }

    override suspend fun readElement(socket: Socket, sendChannel: SendChannel<Double>): Boolean {
        return fileReader.read { buffer ->
            when {
                buffer == null -> {
                    fileReader.close()
                    withDefaultBuffer {
                        Const.Message.DONE.copyInto(array())
                        (local.lastModified() / 1000).toInt().toByteArray().copyInto(array(), destinationOffset = 4)
                        compatLimit(8)
                        socket.writeFully(this)
                    }

                    val transportResponse = socket.readTransportResponse()
                    if (transportResponse.okay) {
                        sendChannel.send(1.0)
                        true
                    } else {
                        throw PushFailedException("adb didn't acknowledge the file transfer: ${transportResponse.message ?: ""}")
                    }
                }
                buffer.limit() > 0 -> {
                    Const.Message.DATA.copyInto(buffer.array())
                    val available = buffer.limit() - 8
                    available.toByteArray().reversedArray().copyInto(buffer.array(), destinationOffset = 4)
                    /**
                     * USB devices are very picky about the size of the DATA buffer. Using the adb's default
                     */
                    socket.writeFully(buffer.array(), 0, available + 8)
                    currentPosition += available
                    sendChannel.send(currentPosition.toDouble() / totalBytes)
                    false
                }
                else -> false
            }
        }
    }

    override fun serialize() = createBaseRequest("sync:")

    override suspend fun close(channel: SendChannel<Double>) {
        fileReader.close()
    }

    override suspend fun writeElement(element: Unit, socket: Socket) = Unit

    override fun validate(): ValidationResponse {
        val response = super.validate()

        val bytes = remotePath.toByteArray(Const.DEFAULT_TRANSPORT_ENCODING)
        return if (!response.success) {
            response
        } else if (!local.exists()) {
            ValidationResponse(false, "Local file $local doesn't exist")
        } else if (!local.isFile) {
            ValidationResponse(false, "$local is not a file")
        } else if (bytes.size > Const.MAX_REMOTE_PATH_LENGTH) {
            ValidationResponse(false, "Remote path should be less that ${Const.MAX_REMOTE_PATH_LENGTH} bytes")
        } else {
            ValidationResponse.Success
        }
    }
}
