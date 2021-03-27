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

package com.malinskiy.roket

import io.ktor.utils.io.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.nio.channels.*
import java.nio.channels.spi.SelectorProvider
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn

class TCPSocket(
    private val socketAddress: InetSocketAddress,
    private val selectorManager: SelectorManager,
    private val connectTimeout: Long,
    val idleTimeout: Long,
    private val selectorProvider: SelectorProvider,
    private val configurationBlock: Socket.() -> Unit
) {

    private lateinit var socketChannel: SocketChannel

    val isClosedForWrite: Boolean
        get() = socketChannel.socket().isOutputShutdown
    val isClosedForRead: Boolean
        get() = socketChannel.socket().isInputShutdown

    suspend fun connect() {
        socketChannel = selectorProvider.openSocketChannel().apply {
            configureBlocking(false)
            configurationBlock(socket())
        }

        doOrCloseByTimeout(connectTimeout) {
            connectOrSuspend()
        }

        socketChannel.finishConnect()
    }

    private suspend fun <T> doOrCloseByTimeout(timoutMillis: Long, block: suspend () -> T): T {
        val result = withTimeoutOrNull(timoutMillis) {
            block()
        }

        if (result == null) {
            close()
            throw SocketTimeoutException("Timeout $idleTimeout reached")
        } else {
            return result
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun connectOrSuspend() {
        return suspendCoroutineUninterceptedOrReturn { continuation: Continuation<Unit> ->
            if (socketChannel.connect(socketAddress)) {
                return@suspendCoroutineUninterceptedOrReturn Unit
            } else {
                selectorManager.send(ConnectMessage(socketChannel, continuation))
            }
            COROUTINE_SUSPENDED
        }
    }

    suspend fun <T> read(block: (ReadableByteChannel) -> T): T {
        return doOrCloseByTimeout(idleTimeout) {
            block(readOrSuspend())
        }
    }

    private suspend fun readOrSuspend(): ReadableByteChannel {
        return suspendCancellableCoroutine { cancellableContinuation ->
            selectorManager.send(ReadMessage(socketChannel, cancellableContinuation))
        }
    }

    suspend fun <T> write(block: (WritableByteChannel) -> T): T {
        return doOrCloseByTimeout(idleTimeout) {
            block(writeOrSuspend())
        }
    }

    private suspend fun writeOrSuspend(): WritableByteChannel {
        return suspendCancellableCoroutine { cancellableContinuation ->
            selectorManager.send(WriteMessage(socketChannel, cancellableContinuation))
        }
    }

    suspend fun close() {
        return suspendCancellableCoroutine { cancellableContinuation ->
            selectorManager.send(DisconnectMessage(socketChannel, cancellableContinuation))
        }
    }
}

class TCPSocketBuilder(
    private val inetSocketAddress: InetSocketAddress,
    private val selectorManager: SelectorManager,
    private val connectTimeout: Long = 10_000,
    private val idleTimeout: Long = 30_000,
) {

    fun build(configurationBlock: Socket.() -> Unit = {}): TCPSocket {
        return TCPSocket(inetSocketAddress, selectorManager, connectTimeout, idleTimeout, selectorProvider, configurationBlock)
    }

    companion object {
        protected val selectorProvider = SelectorProvider.provider()
    }
}