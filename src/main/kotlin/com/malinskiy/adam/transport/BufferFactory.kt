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

import com.malinskiy.adam.Const
import io.ktor.utils.io.pool.*
import java.nio.ByteBuffer

internal const val DEFAULT_BUFFER_SIZE = 4096

val AdamFilePool: ObjectPool<ByteBuffer> = ByteBufferPool(Const.MAX_FILE_PACKET_LENGTH, DEFAULT_BUFFER_SIZE)
val AdamDefaultPool: ObjectPool<ByteBuffer> = ByteBufferPool(Const.DEFAULT_BUFFER_SIZE, DEFAULT_BUFFER_SIZE)

inline fun <R> withDefaultBuffer(block: ByteBuffer.() -> R): R {
    val instance = AdamDefaultPool.borrow()
    return try {
        block(instance)
    } finally {
        AdamDefaultPool.recycle(instance)
    }
}

inline fun <R> withFileBuffer(block: ByteBuffer.() -> R): R {
    val instance = AdamFilePool.borrow()
    return try {
        block(instance)
    } finally {
        AdamFilePool.recycle(instance)
    }
}
