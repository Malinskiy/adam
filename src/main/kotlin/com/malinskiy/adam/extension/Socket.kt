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

package com.malinskiy.adam.extension

import com.malinskiy.adam.Const
import com.malinskiy.adam.request.transform.ResponseTransformer
import com.malinskiy.adam.request.transform.StringResponseTransformer
import com.malinskiy.adam.transport.Socket
import com.malinskiy.adam.transport.TransportResponse
import com.malinskiy.adam.transport.withDefaultBuffer
import com.malinskiy.adam.transport.withFileBuffer
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import io.ktor.utils.io.bits.*
import io.ktor.utils.io.core.*
import java.io.File
import java.nio.ByteBuffer
import kotlin.coroutines.CoroutineContext
import kotlin.text.toByteArray

suspend fun Socket.copyTo(channel: ByteWriteChannel, buffer: ByteArray): Long {
    var processed = 0L
    loop@ while (true) {
        val available = readAvailable(buffer, 0, buffer.size)
        when {
            available < 0 -> {
                break@loop
            }
            available > 0 -> {
                channel.writeFully(buffer, 0, available)
                processed += available
            }
            else -> continue@loop
        }
    }
    return processed
}

/**
 * Copies up to limit bytes into transformer using buffer. If limit is null - copy until EOF
 */
suspend fun <T> Socket.copyTo(transformer: ResponseTransformer<T>, buffer: ByteArray, limit: Long? = null): Long {
    var processed = 0L
    loop@ while (true) {
        val toRead = when {
            limit == null || (limit - processed) > buffer.size -> {
                buffer.size
            }
            else -> {
                (limit - processed).toInt()
            }
        }
        val available = readAvailable(buffer, 0, toRead)
        when {
            processed == limit -> break@loop
            available < 0 -> {
                break@loop
            }
            available > 0 -> {
                transformer.process(buffer, 0, available)
                processed += available
            }
            else -> continue@loop
        }
    }
    return processed
}

/**
 * TODO: rewrite
 * Assumes buffer hasArray == true
 */
suspend fun Socket.copyTo(channel: ByteWriteChannel, buffer: ByteBuffer) = copyTo(channel, buffer.array())
suspend fun <T> Socket.copyTo(transformer: ResponseTransformer<T>, buffer: ByteBuffer) = copyTo(transformer, buffer.array())
suspend fun <T> Socket.copyTo(transformer: ResponseTransformer<T>, buffer: ByteBuffer, limit: Long? = null) =
    copyTo(transformer, buffer.array(), limit)

suspend fun Socket.readOptionalProtocolString(): String? {
    val responseLength = withDefaultBuffer {
        val transformer = StringResponseTransformer()
        copyTo(transformer, this, limit = 4L)
        transformer.transform()
    }
    val errorMessageLength = responseLength.toIntOrNull(16)
    return if (errorMessageLength == null) {
        readStatus()
    } else {
        val errorBytes = ByteArray(errorMessageLength)
        readFully(errorBytes, 0, errorMessageLength)
        String(errorBytes, Const.DEFAULT_TRANSPORT_ENCODING)
    }
}

suspend fun Socket.read(): TransportResponse {
    val bytes = ByteArray(4)
    readFully(bytes, 0, 4)

    val ok = bytes.isOkay()
    val message = if (!ok) {
        readOptionalProtocolString()
    } else {
        null
    }

    return TransportResponse(ok, message)
}

private fun ByteArray.isOkay() = contentEquals(Const.Message.OKAY)

suspend fun Socket.readStatus(): String {
    withDefaultBuffer {
        val transformer = StringResponseTransformer()
        copyTo(transformer, this)
        return transformer.transform()
    }
}

suspend fun Socket.write(request: ByteArray, length: Int? = null) {
    val requestBuffer = ByteBuffer.wrap(request, 0, length ?: request.size)
    writeFully(requestBuffer)
}

suspend fun Socket.writeFile(file: File, coroutineContext: CoroutineContext) = withFileBuffer {
    var fileChannel: ByteReadChannel? = null
    try {
        val fileChannel = file.readChannel(coroutineContext = coroutineContext)
        fileChannel.copyTo(this@writeFile, this)
    } finally {
        fileChannel?.cancel()
    }
}

suspend fun Socket.writeSyncRequest(type: ByteArray, remotePath: String) {
    val path = remotePath.toByteArray(Const.DEFAULT_TRANSPORT_ENCODING)
    val size = path.size.toByteArray().reversedArray()

    val cmd = ByteArray(8 + path.size)

    type.copyInto(cmd)
    size.copyInto(cmd, 4)
    path.copyInto(cmd, 8)
    write(cmd)
}

suspend fun Socket.writeSyncV2Request(type: ByteArray, remotePath: String, flags: Int, mode: Int? = null) {
    val path = remotePath.toByteArray(Const.DEFAULT_TRANSPORT_ENCODING)

    withDefaultBuffer {
        compatRewind()
        compatLimit(4 + 4)
        put(type)
        putInt(path.size.reverseByteOrder())
        compatRewind()
        writeFully(this)

        writeFully(path)

        compatRewind()
        mode?.let { compatLimit(4 + 4 + 4) } ?: compatLimit(4 + 4)
        put(type)
        mode?.let { putInt(it.reverseByteOrder()) }
        putInt(flags.reverseByteOrder())
        compatRewind()
        writeFully(this)
    }
}

suspend fun Socket.readTransportResponse(): TransportResponse {
    val bytes = ByteArray(4)
    readFully(bytes, 0, 4)

    val ok = bytes.isOkay()
    val message = if (!ok) {
        readOptionalProtocolString()
    } else {
        null
    }

    return TransportResponse(ok, message)
}