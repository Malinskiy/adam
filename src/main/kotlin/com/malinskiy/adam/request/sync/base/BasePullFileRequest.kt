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
import com.malinskiy.adam.exception.PullFailedException
import com.malinskiy.adam.exception.UnsupportedSyncProtocolException
import com.malinskiy.adam.extension.toInt
import com.malinskiy.adam.request.AsyncChannelRequest
import com.malinskiy.adam.request.ValidationResponse
import com.malinskiy.adam.request.sync.v1.StatFileRequest
import com.malinskiy.adam.transport.Socket
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.SendChannel
import java.io.File
import kotlin.coroutines.CoroutineContext

/**
 * @param coroutineContext if you don't specify your context then you'll have no control over the `wait for file to finish writing`: closing the channel doesn't close the underlying resources
 */
abstract class BasePullFileRequest(
    private val remotePath: String,
    private val local: File,
    private val size: Long? = null,
    coroutineContext: CoroutineContext = Dispatchers.IO
) : AsyncChannelRequest<Double, Unit>() {

    private val fileWriteChannel = local.also {
        if (!local.exists()) {
            if (!local.parentFile.exists()) {
                local.parentFile.mkdirs()
            }
            local.createNewFile()
        }
    }.writeChannel(coroutineContext = coroutineContext)
    var totalBytes = -1L
    var currentPosition = 0L

    override suspend fun handshake(socket: Socket) {
        super.handshake(socket)
        //If we don't have expected size, fetch it
        totalBytes = size ?: StatFileRequest(remotePath).readElement(socket).size.toLong()
    }

    private val headerBuffer = ByteArray(8)
    private val dataBuffer = ByteArray(Const.MAX_FILE_PACKET_LENGTH)

    override suspend fun readElement(socket: Socket, sendChannel: SendChannel<Double>): Boolean {
        socket.readFully(headerBuffer, 0, 8)

        val header = headerBuffer.copyOfRange(0, 4)
        when {
            header.contentEquals(Const.Message.DONE) -> {
                fileWriteChannel.close()
                return true
            }
            header.contentEquals(Const.Message.DATA) -> {
                val available = headerBuffer.copyOfRange(4, 8).toInt()
                if (available > Const.MAX_FILE_PACKET_LENGTH) {
                    throw UnsupportedSyncProtocolException()
                }
                socket.readFully(dataBuffer, 0, available)
                fileWriteChannel.writeFully(dataBuffer, 0, available)

                currentPosition += available

                sendChannel.send(currentPosition.toDouble() / totalBytes)
            }
            header.contentEquals(Const.Message.FAIL) -> {
                val size = headerBuffer.copyOfRange(4, 8).toInt()
                socket.readFully(dataBuffer, 0, size)
                val errorMessage = String(dataBuffer, 0, size)
                throw PullFailedException("Failed to pull file $remotePath: $errorMessage")
            }
            else -> {
                throw UnsupportedSyncProtocolException("Unexpected header message ${String(header, Const.DEFAULT_TRANSPORT_ENCODING)}")
            }
        }
        return false
    }

    override suspend fun close(channel: SendChannel<Double>) {
        fileWriteChannel.close()
    }

    override fun serialize() = createBaseRequest("sync:")

    override fun validate(): ValidationResponse {
        return if (remotePath.length > Const.MAX_REMOTE_PATH_LENGTH) {
            ValidationResponse(false, "Remote path should be less that ${Const.MAX_REMOTE_PATH_LENGTH} bytes")
        } else {
            ValidationResponse.Success
        }
    }

    override suspend fun writeElement(element: Unit, socket: Socket) = Unit

}
