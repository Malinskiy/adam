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

package com.malinskiy.adam.transport

import java.nio.ByteBuffer

interface Socket : SuspendCloseable {
    val isClosedForWrite: Boolean
    val isClosedForRead: Boolean

    suspend fun writeFully(byteBuffer: ByteBuffer)
    suspend fun writeFully(toByteArray: ByteArray, offset: Int, limit: Int)

    suspend fun readAvailable(buffer: ByteArray, offset: Int, limit: Int): Int
    suspend fun readFully(buffer: ByteBuffer): Int
    suspend fun readFully(buffer: ByteArray, offset: Int, limit: Int)

    suspend fun readByte(): Byte
    suspend fun writeByte(value: Int)

    suspend fun readIntLittleEndian(): Int
    suspend fun writeIntLittleEndian(value: Int)

    suspend fun writeFully(byteArray: ByteArray) = writeFully(byteArray, 0, byteArray.size)
}