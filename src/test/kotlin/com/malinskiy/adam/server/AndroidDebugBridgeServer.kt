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

import com.malinskiy.adam.AndroidDebugBridgeClient
import com.malinskiy.adam.AndroidDebugBridgeClientFactory
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.util.network.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import java.net.InetSocketAddress
import kotlin.coroutines.CoroutineContext


class AndroidDebugBridgeServer : CoroutineScope {
    private val executionDispatcher by lazy {
        newFixedThreadPoolContext(4, "AndroidDebugBridgeServer")
    }
    override val coroutineContext: CoroutineContext
        get() = executionDispatcher

    private val job = SupervisorJob()
    var port: Int = 0

    lateinit var server: ServerSocket

    fun start(): AndroidDebugBridgeClient {
        val address = InetSocketAddress("127.0.0.1", port)
        server = aSocket(ActorSelectorManager(Dispatchers.IO)).tcp().bind(address)
        port = server.localAddress.port

        return AndroidDebugBridgeClientFactory().apply {
            port = this@AndroidDebugBridgeServer.port
        }.build()
    }

    suspend fun startAndListen(block: suspend (ServerReadChannel, ServerWriteChannel) -> Unit): AndroidDebugBridgeClient {
        val client = start()
        listen(block)
        return client
    }

    fun listen(block: suspend (ServerReadChannel, ServerWriteChannel) -> Unit) {
        async(context = job) {
            try {
                val socket = server.accept()
                val input = socket.openReadChannel().toServerReadChannel()
                val output = socket.openWriteChannel(autoFlush = true).toServerWriteChannel()

                try {
                    block(input, output)
                } catch (e: Throwable) {
                    e.printStackTrace()
                } finally {
                    output.close()
                    socket.close()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun dispose() {
        if (job.isActive) {
            job.complete()
            job.join()
        }
    }
}

private fun ByteReadChannel.toServerReadChannel() = ServerReadChannel(this)
private fun ByteWriteChannel.toServerWriteChannel() = ServerWriteChannel(this)
