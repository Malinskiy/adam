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

package com.malinskiy.adam.server.stub

import com.malinskiy.adam.AndroidDebugBridgeClient
import com.malinskiy.adam.AndroidDebugBridgeClientFactory
import com.malinskiy.adam.server.stub.dsl.Expectation
import com.malinskiy.adam.server.stub.dsl.Session
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.ServerSocket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.network.sockets.toJavaAddress
import io.ktor.util.network.port
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.close
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.isActive
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlin.coroutines.CoroutineContext


class AndroidDebugBridgeServer : CoroutineScope {
    private val executionDispatcher by lazy {
        newFixedThreadPoolContext(4, "AndroidDebugBridgeServer")
    }
    override val coroutineContext: CoroutineContext
        get() = executionDispatcher

    val client: AndroidDebugBridgeClient by lazy {
        AndroidDebugBridgeClientFactory().apply {
            port = this@AndroidDebugBridgeServer.port
        }.build()
    }

    private val job = SupervisorJob()
    var port: Int = 0

    lateinit var server: ServerSocket
    lateinit var selector: ActorSelectorManager

    fun start(): AndroidDebugBridgeClient {
        val address = InetSocketAddress("127.0.0.1", port)
        selector = ActorSelectorManager(Dispatchers.IO)
        server = aSocket(selector).tcp().bind(address)
        port = server.localAddress.toJavaAddress().port

        return client
    }

    fun session(block: suspend Session.() -> Unit) {
        listen { input, output ->
            val session = Session(input, output)
            block(session)
        }
    }

    fun multipleSessions(block: suspend Expectation.() -> Unit) {
        async(context = job) {
            val expectation = Expectation()
            block(expectation)

            while (isActive) {
                val socket = server.accept()
                val input = socket.openReadChannel().toServerReadChannel()
                val output = socket.openWriteChannel(autoFlush = true).toServerWriteChannel()

                try {
                    val session = Session(input, output)
                    if (!expectation.select(session)) {
                        throw RuntimeException("No handler registered for request")
                    }
                } catch (e: Throwable) {
                    e.printStackTrace()
                } finally {
                    output.close()
                    socket.close()
                }
            }
        }
    }

    fun listen(block: suspend (input: ServerReadChannel, output: ServerWriteChannel) -> Unit) {
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
            job.children.iterator().forEach {
                if (!it.isCancelled) job.cancelChildren()
            }
            job.join()
        }
        selector.close()
    }
}

private fun ByteReadChannel.toServerReadChannel() = ServerReadChannel(this)
private fun ByteWriteChannel.toServerWriteChannel() = ServerWriteChannel(this)
