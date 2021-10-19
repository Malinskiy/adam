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
import com.malinskiy.adam.extension.compatClear
import org.apache.commons.pool2.PooledObject
import org.apache.commons.pool2.PooledObjectFactory
import org.apache.commons.pool2.impl.DefaultPooledObject
import org.apache.commons.pool2.impl.GenericObjectPool
import org.apache.commons.pool2.impl.GenericObjectPoolConfig
import java.nio.ByteBuffer
import java.nio.ByteOrder

internal const val DEFAULT_BUFFER_SIZE = 4088

val AdamDefaultPool: ByteBufferPool = ByteBufferPool(poolSize = Const.DEFAULT_BUFFER_SIZE, bufferSize = DEFAULT_BUFFER_SIZE)
val AdamMaxPacketPool: ByteBufferPool = ByteBufferPool(poolSize = Const.DEFAULT_BUFFER_SIZE, bufferSize = Const.MAX_PACKET_LENGTH)
val AdamMaxFilePacketPool: ByteBufferPool = ByteBufferPool(poolSize = Const.DEFAULT_BUFFER_SIZE, bufferSize = Const.MAX_FILE_PACKET_LENGTH)

class ByteBufferPool(private val poolSize: Int, private val bufferSize: Int) {
    private val delegate: GenericObjectPool<ByteBuffer> by lazy {
        val config = GenericObjectPoolConfig<ByteBuffer>().apply {
            maxTotal = poolSize
        }
        GenericObjectPool(ByteBufferObjectFactory(bufferSize), config)
    }

    fun borrow(): ByteBuffer = delegate.borrowObject()

    fun recycle(buffer: ByteBuffer) = delegate.returnObject(buffer)
}

class ByteBufferObjectFactory(private val bufferSize: Int) : PooledObjectFactory<ByteBuffer> {
    override fun activateObject(p: PooledObject<ByteBuffer>?) {
        p?.`object`?.apply {
            compatClear()
            order(ByteOrder.BIG_ENDIAN)
        }
    }

    override fun destroyObject(p: PooledObject<ByteBuffer>?) {
        p?.`object`?.apply {
            compatClear()
            order(ByteOrder.BIG_ENDIAN)
        }
    }

    override fun makeObject(): PooledObject<ByteBuffer> {
        return DefaultPooledObject(ByteBuffer.allocate(bufferSize))
    }

    override fun passivateObject(p: PooledObject<ByteBuffer>?) {
        p?.`object`?.apply {
            compatClear()
            order(ByteOrder.BIG_ENDIAN)
        }
    }

    override fun validateObject(p: PooledObject<ByteBuffer>?): Boolean {
        return p?.`object`?.let { instance ->
            instance.capacity() == bufferSize && !instance.isDirect
        } ?: true
    }
}

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

