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
import com.malinskiy.adam.extension.copyTo
import com.malinskiy.adam.extension.readTransportResponse
import com.malinskiy.adam.extension.toByteArray
import com.malinskiy.adam.extension.write
import com.malinskiy.adam.request.AsyncChannelRequest
import com.malinskiy.adam.request.ValidationResponse
import com.malinskiy.adam.transport.Socket
import com.malinskiy.adam.transport.withMaxFilePacketBuffer
import io.ktor.util.cio.*
import io.ktor.utils.io.*
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
    protected val fileReadChannel = local.readChannel(coroutineContext = coroutineContext)
    protected var totalBytes = local.length()
    protected var currentPosition = 0L
    protected val modeValue: Int
        get() = mode.toInt(8) and "0777".toInt(8)

    override suspend fun readElement(socket: Socket, sendChannel: SendChannel<Double>): Boolean {
        withMaxFilePacketBuffer {
            val data = array()
            val available = fileReadChannel.copyTo(data, 0, data.size)
            return when {
                available < 0 -> {
                    Const.Message.DONE.copyInto(data)
                    (local.lastModified() / 1000).toInt().toByteArray().copyInto(data, destinationOffset = 4)
                    socket.write(request = data, length = 8)
                    val transportResponse = socket.readTransportResponse()
                    fileReadChannel.cancel()

                    if (transportResponse.okay) {
                        sendChannel.send(1.0)
                        true
                    } else {
                        throw PushFailedException("adb didn't acknowledge the file transfer: ${transportResponse.message ?: ""}")
                    }
                }
                available > 0 -> {
                    socket.writeFully(Const.Message.DATA)
                    socket.writeFully(available.toByteArray().reversedArray())
                    /**
                     * USB devices are very picky about the size of the DATA buffer. Using the adb's default
                     */
                    /**
                     * USB devices are very picky about the size of the DATA buffer. Using the adb's default
                     */
                    socket.writeFully(data, 0, available)
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
        fileReadChannel.cancel()
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
