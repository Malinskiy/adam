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

import com.malinskiy.adam.transport.Socket
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.yield
import java.nio.ByteBuffer
import kotlin.math.min

suspend fun ByteReadChannel.copyTo(socket: Socket, buffer: ByteBuffer) = copyTo(socket, buffer.array())
suspend fun ByteReadChannel.copyTo(socket: Socket, buffer: ByteArray): Long {
    var processed = 0L
    loop@ while (true) {
        val available = readAvailable(buffer, 0, buffer.size)
        when {
            available < 0 -> {
                break@loop
            }
            available > 0 -> {
                socket.writeFully(buffer, 0, available)
                processed += available
                yield()
            }
            else -> {
                yield()
                continue@loop
            }
        }
    }
    return processed
}

/**
 * Copies up to limit bytes into transformer using buffer. If limit is null - copy until EOF
 */
suspend fun ByteReadChannel.copyTo(buffer: ByteArray, offset: Int, limit: Int): Int {
    var processed = 0
    loop@ while (true) {
        val toRead = min(
            (buffer.size - offset) - processed,
            limit - processed
        )
        val available = readAvailable(buffer, offset + processed, toRead)
        when {
            processed == limit -> break@loop
            available < 0 && processed != 0 -> {
                break@loop
            }
            available < 0 -> return available
            available > 0 -> {
                processed += available
                yield()
            }
            else -> continue@loop
        }
    }
    return processed
}
