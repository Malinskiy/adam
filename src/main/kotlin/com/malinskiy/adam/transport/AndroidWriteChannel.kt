/*
 * Copyright (C) 2019 Anton Malinskiy
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

import com.malinskiy.adam.Const
import com.malinskiy.adam.extension.copyTo
import com.malinskiy.adam.extension.toByteArray
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import java.io.File
import java.nio.ByteBuffer
import kotlin.coroutines.CoroutineContext

class AndroidWriteChannel(private val delegate: ByteWriteChannel) : ByteWriteChannel by delegate {
    suspend fun write(request: ByteArray, length: Int? = null) {
        val requestBuffer = ByteBuffer.wrap(request, 0, length ?: request.size)
        delegate.writeFully(requestBuffer)
    }

    suspend fun writeFile(file: File, coroutineContext: CoroutineContext) = withFileBuffer {
        var fileChannel: ByteReadChannel? = null
        try {
            val fileChannel = file.readChannel(coroutineContext = coroutineContext)
            fileChannel.copyTo(this@AndroidWriteChannel, this)
        } finally {
            fileChannel?.cancel()
        }
    }

    suspend fun writeSyncRequest(type: ByteArray, remotePath: String) {
        val path = remotePath.toByteArray(Const.DEFAULT_TRANSPORT_ENCODING)
        val size = path.size.toByteArray().reversedArray()

        val cmd = ByteArray(8 + path.size)

        type.copyInto(cmd)
        size.copyInto(cmd, 4)
        path.copyInto(cmd, 8)
        write(cmd)
    }
}
