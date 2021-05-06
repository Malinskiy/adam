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
import com.malinskiy.adam.transport.Socket
import com.malinskiy.adam.transport.withDefaultBuffer
import io.vertx.core.Context
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
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
import kotlinx.coroutines.channels.receiveOrNull
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.isActive
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.coroutines.CoroutineContext

class VertxSocket(private val socketAddress: SocketAddress, private val options: NetClientOptions) : CoroutineVerticle(), Socket {
    var id: String? = null
    private lateinit var socket: NetSocketImpl
    private lateinit var recordParser: VariableSizeRecordParser
    private lateinit var readChannel: ReceiveChannel<Buffer>

    override suspend fun start() {
        super.start()
        val client = vertx.createNetClient(options)
        socket = client.connect(socketAddress).await() as NetSocketImpl
        recordParser = VariableSizeRecordParser(stream = socket as ReadStream<Buffer>)
        recordParser.pause()
        readChannel = (recordParser).toChannel(vertx)
    }

    override suspend fun stop() {
        super.stop()
        socket.close().await()
    }

    override val isClosedForWrite: Boolean
        get() {
            val b = !socket.channel().isWritable
            return b
        }
    override val isClosedForRead: Boolean
        get() {
            val b = !socket.channel().isActive && readChannel.isClosedForReceive
            return b
        }

    override suspend fun writeFully(byteBuffer: ByteBuffer) {
        val appendBytes = Buffer.buffer().appendBytes(byteBuffer.array(), byteBuffer.position(), byteBuffer.remaining())
        socket.write(appendBytes).await()
    }

    suspend fun writeFully(buffer: Buffer) {
        socket.write(buffer).await()
    }

    override suspend fun writeFully(byteBuffer: ByteArray, offset: Int, limit: Int) {
        val appendBytes = Buffer.buffer().appendBytes(byteBuffer, offset, limit)
        socket.write(appendBytes).await()
    }

    override suspend fun readAvailable(buffer: ByteArray, offset: Int, limit: Int): Int {
        if (readChannel.isClosedForReceive) return -1
        vertx.runOnContext {
            recordParser.request(limit)
        }
        return readChannel.receiveOrNull()?.let {
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
        withDefaultBuffer {
            while (isActive) {
                val read = readAvailable(array(), 0, limit())
                if (read == -1) break
            }
        }
        id?.let { vertx.undeploy(it) }
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

    override val coroutineContext: CoroutineContext = context.dispatcher()
    fun subscribe() {
        stream.endHandler {
            close()
        }
        stream.exceptionHandler { err ->
            close(err)
        }
        stream.handler { event ->
            if (!isClosedForSend) {
                sendBlocking(event)
                stream.fetch(1)
            } else {
                val length = (event as Buffer).length()
                if (length > 0) println("can't send $length bytes")
            }
        }
    }
}
