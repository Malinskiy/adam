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

import com.malinskiy.adam.Const
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.writeIntLittleEndian
import java.nio.ByteBuffer

class ServerWriteChannel(private val delegate: ByteWriteChannel) : ByteWriteChannel by delegate {
    suspend fun write(request: ByteArray, length: Int? = null) {
        val requestBuffer = ByteBuffer.wrap(request, 0, length ?: request.size)
        delegate.writeFully(requestBuffer)
    }

    suspend fun respond(request: ByteArray, length: Int? = null) = write(request, length)

    suspend fun respondStat(size: Int, mode: Int = 0, lastModified: Int = 0) {
        respond(Const.Message.STAT)
        writeIntLittleEndian(mode)
        writeIntLittleEndian(size)
        writeIntLittleEndian(lastModified)
    }

    suspend fun respondData(byteArray: ByteArray) {
        respond(Const.Message.DATA)
        writeIntLittleEndian(byteArray.size)
        writeFully(byteArray, 0, byteArray.size)
    }

    suspend fun respondDone() {
        respond(Const.Message.DONE)
    }
}