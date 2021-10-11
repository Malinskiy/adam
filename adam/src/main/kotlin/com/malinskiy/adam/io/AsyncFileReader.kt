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

package com.malinskiy.adam.io

import com.malinskiy.adam.Const
import com.malinskiy.adam.extension.compatLimit
import com.malinskiy.adam.request.transform.ResponseTransformer
import com.malinskiy.adam.transport.AdamMaxFilePacketPool
import com.malinskiy.adam.transport.Socket
import com.malinskiy.adam.transport.SuspendCloseable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.nio.ByteBuffer
import kotlin.coroutines.CoroutineContext

/**
 * Reads file using passed coroutineContext and pushes updates of predefined structure using channel
 */
class AsyncFileReader(
    file: File,
    private val start: Long = 0,
    private val offset: Int = 0,
    private val length: Int = Const.MAX_FILE_PACKET_LENGTH,
    override val coroutineContext: CoroutineContext = Dispatchers.IO
) : CoroutineScope, SuspendCloseable {
    private val fileChannel = file.inputStream().buffered()
    private val bufferChannel: Channel<ByteBuffer> = Channel(capacity = 2)
    private var job: Job? = null

    @Suppress("BlockingMethodInNonBlockingContext")
    fun start() {
        job = launch {
            fileChannel.use { readChannel ->
                readChannel.skip(start)
                while (isActive) {
                    var shouldClose = false
                    val byteBuffer = AdamMaxFilePacketPool.borrowObject()
                    when (val read = readChannel.read(byteBuffer.array(), offset, length)) {
                        -1 -> {
                            AdamMaxFilePacketPool.returnObject(byteBuffer)
                            shouldClose = true
                        }
                        else -> {
                            byteBuffer.compatLimit(read + offset)
                            bufferChannel.send(byteBuffer)
                        }
                    }
                    if (shouldClose) close()
                }
            }
        }
    }

    suspend fun <T> read(block: suspend (ByteBuffer?) -> T): T {
        return block(bufferChannel.receiveCatching().getOrNull())
    }

    override suspend fun close() {
        job?.cancel()
        bufferChannel.close()
    }
}

suspend fun AsyncFileReader.copyTo(socket: Socket) {
    while (true) {
        val closed = read {
            try {
                if (it == null) return@read true
                socket.writeFully(it)
                return@read false
            } finally {
                it?.let { buffer -> AdamMaxFilePacketPool.returnObject(buffer) }
            }
        }
        if (closed) break
    }
}

suspend fun <T> AsyncFileReader.copyTo(transformer: ResponseTransformer<T>) {
    while (true) {
        val closed = read {
            try {
                if (it == null) return@read true
                transformer.process(it.array(), it.position(), it.remaining())
                return@read false
            } finally {
                it?.let { buffer -> AdamMaxFilePacketPool.returnObject(buffer) }
            }
        }
        if (closed) break
    }
}
