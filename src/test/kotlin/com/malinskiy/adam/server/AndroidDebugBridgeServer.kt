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
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import kotlinx.coroutines.*
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.coroutines.CoroutineContext
import kotlin.random.Random


class AndroidDebugBridgeServer : CoroutineScope {
    private val executionDispatcher by lazy {
        newFixedThreadPoolContext(4, "AndroidDebugBridgeServer")
    }
    override val coroutineContext: CoroutineContext
        get() = executionDispatcher

    private val job = SupervisorJob()
    var port: Int = 0

    init {
        val ports = generateSequence {
            Random.Default.nextInt(6000, 7000)
        }
        for (port in ports) {
            if (portAvailable(port)) {
                this.port = port
                break
            }
        }
    }

    private fun portAvailable(port: Int): Boolean {
        try {
            Socket("127.0.0.1", port).use({ ignored -> return false })
        } catch (ignored: IOException) {
            return true
        }
    }

    fun startAndListen(block: suspend (ServerReadChannel, ServerWriteChannel) -> Unit) = async(job) {
        val server = aSocket(ActorSelectorManager(Dispatchers.IO)).tcp().bind(InetSocketAddress("127.0.0.1", port))

        while (true) {
            val socket = server.accept()

            async(job) {
                val input = socket.openReadChannel().toServerReadChannel()
                val output = socket.openWriteChannel(autoFlush = true).toServerWriteChannel()

                try {
                    block(input, output)
                } catch (e: Throwable) {
                    e.printStackTrace()
                    socket.close()
                }
            }
        }
    }

    suspend fun dispose() {
        job.cancelAndJoin()
    }

    fun buildClient(): AndroidDebugBridgeClient {
        return AndroidDebugBridgeClientFactory().apply {
            port = this@AndroidDebugBridgeServer.port
        }.build()
    }
}

private fun ByteReadChannel.toServerReadChannel() = ServerReadChannel(this)
private fun ByteWriteChannel.toServerWriteChannel() = ServerWriteChannel(this)
