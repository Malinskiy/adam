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

package com.malinskiy.adam.transport.roket

import com.malinskiy.adam.extension.compatFlip
import com.malinskiy.adam.extension.compatLimit
import com.malinskiy.adam.transport.Socket
import com.malinskiy.adam.transport.withDefaultBuffer
import com.malinskiy.roket.TCPSocket
import java.net.SocketTimeoutException
import java.nio.ByteBuffer
import java.nio.ByteOrder

class Roket(private val socket: TCPSocket) : Socket {
    override val isClosedForWrite: Boolean
        get() = socket.isClosedForWrite
    override val isClosedForRead: Boolean
        get() = socket.isClosedForRead

    override suspend fun readAvailable(buffer: ByteArray, offset: Int, limit: Int): Int {
        return socket.read {
            it.read(ByteBuffer.wrap(buffer, offset, limit))
        }
    }

    override suspend fun readFully(buffer: ByteBuffer): Int {
        var size = 0
        var timeoutWindowEnd = updateTimeoutWindow()
        while (buffer.hasRemaining()) {
            val read = socket.read {
                it.read(buffer)
            }
            if (read > 0) {
                timeoutWindowEnd = updateTimeoutWindow()
            }
            size += read
            if (System.currentTimeMillis() >= timeoutWindowEnd) {
                throw SocketTimeoutException("Timeout ${socket.idleTimeout}ms reached")
            }
        }
        return size
    }

    override suspend fun writeFully(byteBuffer: ByteBuffer) {
        while (byteBuffer.hasRemaining()) {
            socket.write {
                it.write(byteBuffer)
            }
        }
    }

    override suspend fun writeFully(toByteArray: ByteArray, offset: Int, limit: Int) {
        writeFully(ByteBuffer.wrap(toByteArray, offset, limit))
    }

    private fun updateTimeoutWindow() = System.currentTimeMillis() + socket.idleTimeout

    override suspend fun readFully(buffer: ByteArray, offset: Int, limit: Int) {
        val byteBuffer = ByteBuffer.wrap(buffer, offset, limit)
        readFully(byteBuffer)
    }

    override suspend fun readByte(): Byte {
        withDefaultBuffer {
            compatLimit(1)
            readFully(this)
            compatFlip()
            return get()
        }
    }

    override suspend fun writeByte(value: Int) {
        withDefaultBuffer {
            put(value.toByte())
            compatFlip()
            writeFully(this)
        }
    }

    override suspend fun readIntLittleEndian(): Int {
        withDefaultBuffer {
            order(ByteOrder.LITTLE_ENDIAN)
            compatLimit(4)
            readFully(this)
            compatFlip()
            return int
        }
    }

    override suspend fun writeIntLittleEndian(value: Int) {
        withDefaultBuffer {
            order(ByteOrder.LITTLE_ENDIAN)
            putInt(value)
            compatFlip()
            writeFully(this)
        }
    }

    override suspend fun close() {
        socket.close()
    }
}