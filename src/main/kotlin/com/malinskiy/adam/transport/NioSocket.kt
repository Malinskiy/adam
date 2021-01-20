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

import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.yield
import java.io.IOException
import java.net.InetSocketAddress
import java.net.SocketTimeoutException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.SocketChannel
import java.nio.channels.spi.SelectorProvider
import java.util.concurrent.atomic.AtomicReference

class NioSocket(
    private val socketAddress: InetSocketAddress,
    private val connectTimeout: Long,
    private val idleTimeout: Long,
) : Socket {
    private val state = AtomicReference(State.CLOSED)
    private val mutex = Mutex()

    override val isClosedForWrite: Boolean
        get() = socketChannel.socket().isOutputShutdown || state.get() == State.CLOSING
    override val isClosedForRead: Boolean
        get() = socketChannel.socket().isInputShutdown || state.get() == State.CLOSE_WAIT

    private lateinit var selector: Selector
    private lateinit var socketChannel: SocketChannel
    private lateinit var selectionKey: SelectionKey

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun connect() {
        if (!state.compareAndSet(State.CLOSED, State.SYN_SENT)) return

        socketChannel = SelectorProvider.provider().openSocketChannel().apply {
            configureBlocking(false)
            configure(socket())
        }

        val success = socketChannel.connect(socketAddress)
        selector = SelectorProvider.provider().openSelector()
        if (success) {
            processAccept(selector)
        } else {
            processConnect(selector)
        }
    }

    private fun configure(socket: java.net.Socket) {
        socket.tcpNoDelay = true
    }

    override suspend fun writeFully(byteBuffer: ByteBuffer) {
        if (state.get() != State.ESTABLISHED) return

        processWrite(selector, byteBuffer)
    }

    override suspend fun writeFully(toByteArray: ByteArray, offset: Int, limit: Int) =
        writeFully(ByteBuffer.wrap(toByteArray, offset, limit))

    override suspend fun readAvailable(buffer: ByteArray, offset: Int, limit: Int): Int =
        readAvailable(ByteBuffer.wrap(buffer, offset, limit))

    suspend fun readAvailable(buffer: ByteBuffer): Int {
        if (isClosedForRead) return -1

        return processRead(selector, buffer)
    }

    override suspend fun readFully(buffer: ByteBuffer): Int {
        var remaining = buffer.limit()

        return withTimeoutOrNull(idleTimeout) {
            while (remaining != 0) {
                when (val read = readAvailable(buffer)) {
                    -1 -> {
                        if (remaining == buffer.limit()) return@withTimeoutOrNull -1
                    }
                    0 -> Unit
                    else -> {
                        remaining -= read
                    }
                }
                yield()
            }

            return@withTimeoutOrNull remaining
        } ?: throw SocketTimeoutException("Timeout reading")
    }

    override suspend fun readFully(buffer: ByteArray, offset: Int, limit: Int) {
        readFully(ByteBuffer.wrap(buffer, offset, limit))
    }

    override suspend fun readByte(): Byte {
        val buffer = ByteBuffer.allocate(1)
        val read = readFully(buffer)
        //TODO: handle EOF
        return buffer.array()[0]
    }

    override suspend fun writeByte(value: Int) {
        writeFully(ByteArray(1) { value.toByte() })
    }

    override suspend fun readIntLittleEndian(): Int {
        val allocate = ByteBuffer.allocate(4)
        allocate.order(ByteOrder.LITTLE_ENDIAN)
        val read = readFully(allocate)
        allocate.flip()
        return allocate.int
    }

    override suspend fun writeIntLittleEndian(value: Int) {
        val allocate = ByteBuffer.allocate(4)
        allocate.order(ByteOrder.LITTLE_ENDIAN)
        allocate.putInt(value)
        allocate.flip()
        writeFully(allocate)
    }

    override suspend fun close() {
        mutex.withLock {
            val shouldDrain = when {
                state.compareAndSet(State.ESTABLISHED, State.CLOSING) -> {
                    true
                }
                state.compareAndSet(State.CLOSE_WAIT, State.CLOSING) -> {
                    false
                }
                else -> {
                    return
                }
            }

            if (!socketChannel.socket().isOutputShutdown) {
                socketChannel.socket().shutdownOutput()
            }

            if (shouldDrain) {
                val buffer = ByteBuffer.allocate(128)
                while (true) {
                    buffer.clear()
                    if (readUnsafe(selector, buffer) == -1 || state.get() == State.CLOSED || isClosedForRead) {
                        break
                    } else {
                        yield()
                    }
                }
            }

            state.compareAndSet(State.CLOSING, State.CLOSED)
            if (!socketChannel.socket().isInputShutdown) {
                socketChannel.socket().shutdownInput()
            }
            selectionKey.cancel()
            selector.close()
            socketChannel.close()
            socketChannel.socket().close()
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun processConnect(selector: Selector) {
        mutex.withLock {
            selectionKey = socketChannel.register(selector, SelectionKey.OP_CONNECT)
            withTimeoutOrNull(connectTimeout) {
                while (isActive) {
                    if (selector.selectNow() == 0) yield()
                    val iterator = selector.selectedKeys().iterator()
                    while (iterator.hasNext()) {
                        val selectionKey = iterator.next()
                        if (selectionKey.isConnectable) {
                            socketChannel.finishConnect()
                            selectionKey.interestOps(0)

                            val success = state.compareAndSet(State.SYN_SENT, State.ESTABLISHED)
                            if (!success) throw IllegalStateException("Invalid state ${state.get()} after connect")
                            iterator.remove()

                            return@withTimeoutOrNull
                        }
                    }
                }
            }
            selectionKey.interestOps(0)
            if (socketChannel.isConnectionPending) {
                try {
                    socketChannel.close()
                } catch (e: IOException) {
                    //ignore
                }
                throw SocketTimeoutException("Channel $socketChannel timeout while connecting. Closing")
            }
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun processAccept(selector: Selector) {
        mutex.withLock {
            selectionKey = socketChannel.register(selector, 0)
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun processRead(selector: Selector, buffer: ByteBuffer): Int {
        mutex.withLock {
            if (state.get() != State.ESTABLISHED) return 0
            return readUnsafe(selector, buffer)
        }
    }

    private fun readUnsafe(selector: Selector, buffer: ByteBuffer): Int {
        selectionKey.interestOps(SelectionKey.OP_READ)

        selector.selectNow()
        val selectedKeys = when {
            selector.selectedKeys().isNotEmpty() -> selector.selectedKeys()
            else -> {
                selectionKey.interestOps(0)
                return 0
            }
        }

        val iterator = selectedKeys.iterator()
        while (iterator.hasNext()) {
            val selectionKey = iterator.next()
            if (selectionKey.isReadable) {
                val read = socketChannel.read(buffer)
                if (read == -1) {
                    when (state.get()) {
                        State.ESTABLISHED -> {
                            state.set(State.CLOSE_WAIT)
                        }
                        State.CLOSING -> state.set(State.CLOSED)
                    }
                }
                selectionKey.interestOps(0)
                return read
            }
        }

        selectionKey.interestOps(0)
        return 0
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun processWrite(selector: Selector, buffer: ByteBuffer) {
        mutex.withLock {
            if (state.get() != State.ESTABLISHED) return

            selectionKey.interestOps(SelectionKey.OP_WRITE)

            var remaining = buffer.limit()
            val timeout = withTimeoutOrNull(idleTimeout) {
                while (true) {
                    selector.selectNow()

                    val selectedKeys = when {
                        selector.selectedKeys().isNotEmpty() -> selector.selectedKeys()
                        else -> {
                            yield()
                            continue
                        }
                    }

                    val iterator = selectedKeys.iterator()
                    var processed = false
                    while (iterator.hasNext()) {
                        val selectionKey = iterator.next()
                        if (selectionKey.isWritable) {
                            iterator.remove()
                            val written = socketChannel.write(buffer)
                            remaining -= written
                            if (written != 0) {
                                processed = true
                            }
                            if (remaining == 0) {
                                selectionKey.interestOps(0)
                                return@withTimeoutOrNull
                            }
                        }
                    }
                    if (!processed) yield()
                }
            }
            selectionKey.interestOps(0)
            if (timeout == null) {
                throw SocketTimeoutException("Timeout writing")
            }
        }
    }

    private enum class State {
        CLOSED,
        SYN_SENT,
        ESTABLISHED,
        CLOSING,
        CLOSE_WAIT
    }
}
