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

package com.malinskiy.adam.transport.ktor

import com.malinskiy.adam.log.AdamLogging
import com.malinskiy.adam.transport.Socket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.cancel
import io.ktor.utils.io.close
import io.ktor.utils.io.readIntLittleEndian
import io.ktor.utils.io.writeByte
import io.ktor.utils.io.writeIntLittleEndian
import java.nio.ByteBuffer
import io.ktor.network.sockets.Socket as RealKtorSocket

class KtorSocket(private val ktorSocket: RealKtorSocket) : Socket {
    private val readChannel: ByteReadChannel = ktorSocket.openReadChannel()
    private val writeChannel: ByteWriteChannel = ktorSocket.openWriteChannel(autoFlush = true)
    override val isClosedForWrite: Boolean
        get() = writeChannel.isClosedForWrite
    override val isClosedForRead: Boolean
        get() = readChannel.isClosedForRead

    override suspend fun readFully(buffer: ByteBuffer): Int = readChannel.readFully(buffer)
    override suspend fun readFully(buffer: ByteArray, offset: Int, limit: Int) = readChannel.readFully(buffer, offset, limit)
    override suspend fun writeFully(byteBuffer: ByteBuffer) = writeChannel.writeFully(byteBuffer)
    override suspend fun writeFully(byteArray: ByteArray, offset: Int, limit: Int) = writeChannel.writeFully(byteArray, offset, limit)
    override suspend fun readAvailable(buffer: ByteArray, offset: Int, limit: Int): Int {
        if (!readChannel.isClosedForRead && readChannel.availableForRead == 0) return 0
        return readChannel.readAvailable(buffer, offset, limit)
    }

    override suspend fun readByte(): Byte = readChannel.readByte()
    override suspend fun readIntLittleEndian(): Int = readChannel.readIntLittleEndian()
    override suspend fun writeByte(value: Int) = writeChannel.writeByte(value)
    override suspend fun writeIntLittleEndian(value: Int) = writeChannel.writeIntLittleEndian(value)

    override suspend fun close() {
        try {
            writeChannel.close()
            readChannel.cancel()
            ktorSocket.close()
        } catch (e: Exception) {
            log.debug(e) { "Exception during cleanup. Ignoring" }
        }
    }

    companion object {
        private val log = AdamLogging.logger {}
    }
}
