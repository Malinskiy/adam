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

import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.yield
import java.net.InetSocketAddress
import java.net.SocketTimeoutException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.SocketChannel
import java.util.concurrent.atomic.AtomicReference

class NioSocket(
    private val socketInetAddress: InetSocketAddress,
    private val socketTimeout: Long,
    private val selector: Selector
) : Socket {
    private val state = AtomicReference(State.CLOSED)

    override val isClosedForWrite: Boolean
        get() = socketChannel.socket().isOutputShutdown || state.get() == State.CLOSING
    override val isClosedForRead: Boolean
        get() = socketChannel.socket().isInputShutdown || state.get() == State.CLOSE_WAIT

    private lateinit var socketChannel: SocketChannel
    private lateinit var selectionKey: SelectionKey

    suspend fun connect() {
        if (state.get() != State.CLOSED) return
        state.set(State.SYN_SENT)

        socketChannel = SocketChannel.open().apply {
            configureBlocking(false)
            configure(socket())
        }

        val success = socketChannel.connect(socketInetAddress)
        if (!success) {
            selectionKey = socketChannel.register(selector, SelectionKey.OP_CONNECT)
            withTimeoutOrNull(socketTimeout) {
                while (true) {
                    if (selector.selectNow() == 0) yield()
                    val iterator = selector.selectedKeys().iterator()
                    while (iterator.hasNext()) {
                        val selectionKey = iterator.next()
                        if (selectionKey.isConnectable) {
                            socketChannel.finishConnect()
                            selectionKey.interestOps(0)

                            state.compareAndSet(State.SYN_SENT, State.ESTABLISHED)
                            iterator.remove()

                            return@withTimeoutOrNull
                        }
                    }
                }
            } ?: throw SocketTimeoutException("Timeout connecting to socket")
        }
    }

    private fun configure(socket: java.net.Socket) {
    }

    override suspend fun writeFully(byteBuffer: ByteBuffer) {
        if (state.get() != State.ESTABLISHED) return

        selectionKey.interestOps(SelectionKey.OP_WRITE)

        var remaining = byteBuffer.limit()
        withTimeoutOrNull(socketTimeout) {
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
                        val written = socketChannel.write(byteBuffer)
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
        } ?: throw SocketTimeoutException("Timeout writing")
    }

    override suspend fun writeFully(toByteArray: ByteArray, offset: Int, limit: Int) =
        writeFully(ByteBuffer.wrap(toByteArray, offset, limit))

    override suspend fun readAvailable(buffer: ByteArray, offset: Int, limit: Int): Int =
        readAvailable(ByteBuffer.wrap(buffer, offset, limit))

    fun readAvailable(buffer: ByteBuffer): Int {
        if (isClosedForRead) return -1

        selectionKey.interestOps(SelectionKey.OP_READ)

        selector.selectNow()
        val selectedKeys = when {
            selector.selectedKeys().isNotEmpty() -> selector.selectedKeys()
            else -> return 0
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

        return 0
    }

    override suspend fun readFully(buffer: ByteBuffer): Int {
        var remaining = buffer.limit()

        return withTimeoutOrNull(socketTimeout) {
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
        if (state.get() == State.CLOSE_WAIT || state.compareAndSet(State.ESTABLISHED, State.CLOSING)) {
            socketChannel.shutdownOutput()

            drainSelector()

            state.compareAndSet(State.CLOSING, State.CLOSED)
            socketChannel.shutdownInput()
            socketChannel.close()
            selectionKey.cancel()
        }
    }

    private suspend fun drainSelector() {
        val buffer = ByteBuffer.allocate(128)

//        withTimeoutOrNull(socketTimeout) {
        while (true) {
            buffer.clear()
            if (readAvailable(buffer) == -1 || state.get() == State.CLOSED) {
                return
            } else {
                yield()
            }

            val readyOps = selectionKey.readyOps()
            when {
                readyOps and SelectionKey.OP_CONNECT != 0 -> {
                    selectionKey.interestOps(selectionKey.interestOps() or SelectionKey.OP_CONNECT)
                    selector.selectNow()
                    val iterator = selector.selectedKeys().iterator()
                    while (iterator.hasNext()) {
                        val selectionKey = iterator.next()
                        if (selectionKey.isConnectable) {
                            selectionKey.interestOps(0)
                            iterator.remove()
                        }
                    }
                }
                readyOps and SelectionKey.OP_WRITE != 0 -> {
                    selectionKey.interestOps(selectionKey.interestOps() or SelectionKey.OP_WRITE)
                    selector.selectNow()
                    val iterator = selector.selectedKeys().iterator()
                    while (iterator.hasNext()) {
                        val selectionKey = iterator.next()
                        if (selectionKey.isWritable) {
                            selectionKey.interestOps(0)
                            socketChannel.write(ByteBuffer.wrap(ByteArray(0)))
                            iterator.remove()
                        }
                    }
                }
            }
//            }
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
