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

import com.malinskiy.adam.transport.AdamMaxFilePacketPool
import com.malinskiy.adam.transport.SuspendCloseable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.io.File
import java.nio.ByteBuffer
import kotlin.coroutines.CoroutineContext

class AsyncFileWriter(
    file: File,
    override val coroutineContext: CoroutineContext = Dispatchers.IO
) : CoroutineScope, SuspendCloseable {
    private val fileChannel by lazy {
        if (!file.exists()) {
            if (!file.parentFile.exists()) {
                file.parentFile.mkdirs()
            }
            file.createNewFile()
        }
        file.outputStream().buffered()
    }
    private val bufferChannel: Channel<ByteBuffer> = Channel(capacity = 2)
    private var job: Job? = null

    @Suppress("BlockingMethodInNonBlockingContext")
    fun start() {
        job = launch {
            for (buffer in bufferChannel) {
                fileChannel.write(buffer.array(), buffer.position(), buffer.limit())
                AdamMaxFilePacketPool.returnObject(buffer)
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun write(byteBuffer: ByteBuffer) {
        bufferChannel.send(byteBuffer)
    }

    override suspend fun close() {
        bufferChannel.close()
        job?.join()
        fileChannel.close()
    }
}
