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

import java.io.Closeable
import java.nio.ByteBuffer

interface Socket : Closeable {
    val availableForRead: Int
    val isClosedForWrite: Boolean
    val isClosedForRead: Boolean

    suspend fun readFully(sizeBuffer: ByteBuffer): Int
    suspend fun readFully(buffer: ByteArray, offset: Int, limit: Int)
    suspend fun writeFully(toByteArray: ByteArray)
    suspend fun writeFully(byteBuffer: ByteBuffer)
    suspend fun writeFully(toByteArray: ByteArray, offset: Int, limit: Int)
    suspend fun readAvailable(buffer: ByteArray, offset: Int, limit: Int): Int
    suspend fun readByte(): Byte
    suspend fun readIntLittleEndian(): Int
    suspend fun writeByte(toValue: Int)
    suspend fun writeIntLittleEndian(size: Int)
}