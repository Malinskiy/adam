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
import io.ktor.utils.io.pool.ByteBufferPool
import io.ktor.utils.io.pool.ObjectPool
import java.nio.ByteBuffer

internal const val DEFAULT_BUFFER_SIZE = 4088

val AdamDefaultPool: ObjectPool<ByteBuffer> = ByteBufferPool(Const.DEFAULT_BUFFER_SIZE, bufferSize = DEFAULT_BUFFER_SIZE)
val AdamMaxPacketPool: ObjectPool<ByteBuffer> = ByteBufferPool(Const.DEFAULT_BUFFER_SIZE, bufferSize = Const.MAX_PACKET_LENGTH)
val AdamMaxFilePacketPool: ObjectPool<ByteBuffer> = ByteBufferPool(Const.DEFAULT_BUFFER_SIZE, bufferSize = Const.MAX_FILE_PACKET_LENGTH)

inline fun <R> withDefaultBuffer(block: ByteBuffer.() -> R): R {
    val instance = AdamDefaultPool.borrow()
    return try {
        block(instance)
    } finally {
        AdamDefaultPool.recycle(instance)
    }
}

inline fun <R> withMaxPacketBuffer(block: ByteBuffer.() -> R): R {
    val instance = AdamMaxPacketPool.borrow()
    return try {
        block(instance)
    } finally {
        AdamMaxPacketPool.recycle(instance)
    }
}

inline fun <R> withMaxFilePacketBuffer(block: ByteBuffer.() -> R): R {
    val instance = AdamMaxFilePacketPool.borrow()
    return try {
        block(instance)
    } finally {
        AdamMaxFilePacketPool.recycle(instance)
    }
}

