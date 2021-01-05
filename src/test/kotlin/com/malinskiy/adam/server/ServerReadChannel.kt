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

package com.malinskiy.adam.server

import com.malinskiy.adam.Const
import com.malinskiy.adam.exception.UnsupportedSyncProtocolException
import com.malinskiy.adam.extension.toInt
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import java.io.File

class ServerReadChannel(private val delegate: ByteReadChannel) : ByteReadChannel by delegate {
    suspend fun receiveCommand(): String {
        val bytes = ByteArray(4)
        readFully(bytes, 0, 4)
        val length = String(bytes, Const.DEFAULT_TRANSPORT_ENCODING).toInt(16)
        val request = ByteArray(length)
        readFully(request, 0, length)
        return String(request, Const.DEFAULT_TRANSPORT_ENCODING)
    }

    suspend fun receiveBytes(length: Int): ByteArray {
        val bytes = ByteArray(length)
        readFully(bytes, 0, length)
        return bytes
    }

    suspend fun receiveStat(): String {
        val protocolMessage = receiveProtocolMessage()
        val message = String(protocolMessage, Const.DEFAULT_TRANSPORT_ENCODING)
        if (message != "STAT") throw RuntimeException(
            "Unexpected protocol message $message"
        )
        val size = readIntLittleEndian()
        val request = ByteArray(size)
        readFully(request, 0, size)
        return String(request, Const.DEFAULT_TRANSPORT_ENCODING)
    }

    suspend fun receiveList(): String {
        val protocolMessage = receiveProtocolMessage()
        val message = String(protocolMessage, Const.DEFAULT_TRANSPORT_ENCODING)
        if (message != "LIST") throw RuntimeException(
            "Unexpected protocol message $message"
        )
        val size = readIntLittleEndian()
        val request = ByteArray(size)
        readFully(request, 0, size)
        return String(request, Const.DEFAULT_TRANSPORT_ENCODING)
    }

    suspend fun receiveListV2(): String {
        val protocolMessage = receiveProtocolMessage()
        val message = String(protocolMessage, Const.DEFAULT_TRANSPORT_ENCODING)
        if (message != "LIS2") throw RuntimeException(
            "Unexpected protocol message $message"
        )
        val size = readIntLittleEndian()
        val request = ByteArray(size)
        readFully(request, 0, size)
        return String(request, Const.DEFAULT_TRANSPORT_ENCODING)
    }

    suspend fun receiveSend(): String {
        val protocolMessage = receiveProtocolMessage()
        val message = String(protocolMessage, Const.DEFAULT_TRANSPORT_ENCODING)
        if (message != "SEND") throw RuntimeException(
            "Unexpected protocol message $message"
        )
        val size = readIntLittleEndian()
        val request = ByteArray(size)
        readFully(request, 0, size)
        return String(request, Const.DEFAULT_TRANSPORT_ENCODING)
    }

    suspend fun receiveProtocolMessage(): ByteArray {
        val bytes = ByteArray(4)
        readFully(bytes, 0, 4)
        return bytes
    }

    suspend fun receiveRecv(): String {
        val protocolMessage = receiveProtocolMessage()
        val message = String(protocolMessage, Const.DEFAULT_TRANSPORT_ENCODING)
        if (message != "RECV") throw RuntimeException(
            "Unexpected protocol message $message"
        )

        val size = readIntLittleEndian()
        val request = ByteArray(size)
        readFully(request, 0, size)
        return String(request, Const.DEFAULT_TRANSPORT_ENCODING)
    }

    suspend fun receiveFile(): File {
        val tempFile = createTempFile()
        val channel = tempFile.writeChannel()

        val headerBuffer = ByteArray(8)
        val dataBuffer = ByteArray(Const.MAX_FILE_PACKET_LENGTH)
        while (true) {
            readFully(headerBuffer, 0, 8)
            val header = headerBuffer.copyOfRange(0, 4)

            when {
                header.contentEquals(Const.Message.DONE) -> {
                    return tempFile
                }
                header.contentEquals(Const.Message.DATA) -> {
                    val available = headerBuffer.copyOfRange(4, 8).toInt()
                    if (available > Const.MAX_FILE_PACKET_LENGTH) {
                        throw UnsupportedSyncProtocolException()
                    }
                    readFully(dataBuffer, 0, available)
                    channel.writeFully(dataBuffer, 0, available)
                    channel.flush()
                    channel.close()
                }
                else -> throw RuntimeException("Something bad happened")
            }
        }
    }
}
