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

package com.malinskiy.adam.transport.vertx

import com.malinskiy.adam.extension.compatFlip
import com.malinskiy.adam.extension.compatLimit
import com.malinskiy.adam.extension.compatPosition
import com.malinskiy.adam.log.AdamLogging
import com.malinskiy.adam.transport.Socket
import com.malinskiy.adam.transport.withDefaultBuffer
import com.malinskiy.adam.transport.withMaxFilePacketBuffer
import io.vertx.core.Context
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.net.NetClient
import io.vertx.core.net.NetClientOptions
import io.vertx.core.net.SocketAddress
import io.vertx.core.net.impl.NetSocketImpl
import io.vertx.core.streams.ReadStream
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.isActive
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.CoroutineContext

class VertxSocket(private val socketAddress: SocketAddress, private val options: NetClientOptions) : CoroutineVerticle(), Socket {
    var id: String? = null
    var netClient: NetClient? = null
    private lateinit var socket: NetSocketImpl
    private lateinit var recordParser: VariableSizeRecordParser
    private lateinit var readChannel: ReceiveChannel<Buffer>
    private val state = AtomicReference(State.CREATED)
    private val canRead = AtomicBoolean(false)

    override suspend fun start() {
        super.start()
        val client = vertx.createNetClient(options)
        netClient = client
        val connect = client.connect(socketAddress)
        state.change(State.CREATED, State.SYN_SENT) { "Socket connection error" }
        socket = connect.await() as NetSocketImpl

        state.change(State.SYN_SENT, State.ESTABLISHED) { "Socket connection error" }
        canRead.flip(false) { "Socket connection error: canRead was true, expected false" }

        socket
            .endHandler {
                canRead.flip(true) { "Socket close error: canRead was false, expected true" }
            }
            .closeHandler {
                state.change(State.ESTABLISHED, State.CLOSED) { "Socket close error" }
            }

        recordParser = VariableSizeRecordParser(stream = socket as ReadStream<Buffer>)
        recordParser.pause()
        readChannel = (recordParser).toChannel(vertx)
    }

    override suspend fun stop() {
        super.stop()
        netClient?.close()
    }

    override val isClosedForWrite: Boolean
        get() {
            return state.get() != State.ESTABLISHED
        }
    override val isClosedForRead: Boolean
        get() {
            return !canRead.get() || readChannel.isClosedForReceive
        }

    override suspend fun writeFully(byteBuffer: ByteBuffer) {
        if (isClosedForWrite) throw IllegalStateException("Socket write error: socket is not connected ${state.get()}")
        val appendBytes = Buffer.buffer().appendBytes(byteBuffer.array(), byteBuffer.position(), byteBuffer.remaining())
        socket.write(appendBytes).await()
    }

    suspend fun writeFully(buffer: Buffer) {
        if (isClosedForWrite) throw IllegalStateException("Socket write error: socket is not connected ${state.get()}")
        socket.write(buffer).await()
    }

    override suspend fun writeFully(byteArray: ByteArray, offset: Int, limit: Int) {
        if (isClosedForWrite) throw IllegalStateException("Socket write error: socket is not connected ${state.get()}")
        val appendBytes = Buffer.buffer().appendBytes(byteArray, offset, limit)
        socket.write(appendBytes).await()
    }

    override suspend fun readAvailable(buffer: ByteArray, offset: Int, limit: Int): Int {
        if (readChannel.isClosedForReceive) return -1
        vertx.runOnContext {
            recordParser.request(limit)
        }
        return readChannel.receiveCatching().getOrNull()?.let {
            val actualLimit = it.byteBuf.writerIndex()
            assert(actualLimit <= limit) { "Received ${it.length()} more than we can chew $limit" }
            it.getBytes(0, actualLimit, buffer, offset)
            actualLimit
        } ?: -1
    }

    override suspend fun readFully(buffer: ByteBuffer): Int {
        val offset = buffer.position()
        val limit = buffer.remaining()
        var total = 0
        while (total != limit) {
            val read = readAvailable(buffer.array(), offset + total, limit - total)
            if (read == -1) {
                assert(!buffer.hasRemaining()) { "Expected $limit bytes, received $total" }
            } else {
                total += read
            }
        }

        buffer.compatPosition(buffer.position() + total)
        return limit
    }

    override suspend fun readFully(buffer: ByteArray, offset: Int, limit: Int) {
        readFully(ByteBuffer.wrap(buffer, offset, limit))
    }

    override suspend fun readByte(): Byte {
        withDefaultBuffer {
            compatLimit(1)
            readFully(this)
            compatFlip()
            return get()
        }
    }

    override suspend fun writeByte(value: Int) {
        withDefaultBuffer {
            put(value.toByte())
            compatFlip()
            writeFully(this)
        }
    }

    override suspend fun readIntLittleEndian(): Int {
        withDefaultBuffer {
            order(ByteOrder.LITTLE_ENDIAN)
            compatLimit(4)
            readFully(this)
            compatFlip()
            return int
        }
    }

    override suspend fun writeIntLittleEndian(value: Int) {
        withDefaultBuffer {
            order(ByteOrder.LITTLE_ENDIAN)
            putInt(value)
            compatFlip()
            writeFully(this)
        }
    }

    override suspend fun close() {
        socket.close().await()
        /**
         * This should be maximum buffer that could've been requested in case:
         * 1. Read request for up to max initiated
         * 2. Actual read doesn't happen due to exception of cancellation
         * 3. Close tries to read and channel already contains result of read from 1
         */
        withMaxFilePacketBuffer {
            while (isActive) {
                val read = readAvailable(array(), 0, limit())
                if (read == -1) break
            }
        }
        id?.let {
            vertx.undeploy(it).await()
        }
    }
}

fun <T> ReadStream<T>.toChannel(vertx: Vertx): ReceiveChannel<T> {
    return toChannel(vertx.orCreateContext)
}

/**
 * Adapts the current read stream to Kotlin [ReceiveChannel].
 *
 * @param context the vertx context
 */
fun <T> ReadStream<T>.toChannel(context: Context): ReceiveChannel<T> {
    this.pause()
    val ret = ChannelReadStream(
        stream = this,
        channel = Channel(16),
        context = context
    )
    ret.subscribe()
    this.fetch(1)
    return ret
}

private class ChannelReadStream<T>(
    val stream: ReadStream<T>,
    val channel: Channel<T>,
    context: Context
) : Channel<T> by channel, CoroutineScope {

    private val logger = AdamLogging.logger { }

    override val coroutineContext: CoroutineContext = context.dispatcher()
    fun subscribe() {
        stream
            .endHandler {
                close()
            }
            .exceptionHandler { err ->
                close(err)
            }
            .handler { event ->
                if (!isClosedForSend) {
                    trySendBlocking(event)
                    stream.fetch(1)
                } else {
                    val length = (event as Buffer).length()
                    if (length > 0) logger.debug { "can't send $length bytes" }
                }
            }
    }
}

private enum class State {
    CREATED,
    CLOSED,
    SYN_SENT,
    ESTABLISHED,
    CLOSING,
    CLOSE_WAIT
}

private fun AtomicBoolean.flip(expected: Boolean, error: () -> String) {
    if (!compareAndSet(expected, !expected)) {
        throw IllegalStateException(error.invoke())
    }
}

private fun AtomicReference<State>.change(expected: State, new: State, error: () -> String) {
    if (!compareAndSet(expected, new)) {
        throw IllegalStateException("${error.invoke()}: ${get()}, expected $expected")
    }
}
