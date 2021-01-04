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
import io.ktor.utils.io.*
import java.nio.ByteBuffer

class ServerWriteChannel(private val delegate: ByteWriteChannel) : ByteWriteChannel by delegate {
    private suspend fun write(request: ByteArray, length: Int? = null) {
        val requestBuffer = ByteBuffer.wrap(request, 0, length ?: request.size)
        delegate.writeFully(requestBuffer)
    }

    suspend fun respond(request: ByteArray, length: Int? = null) = write(request, length)

    suspend fun respondOkay() {
        respond(Const.Message.OKAY)
    }

    suspend fun respondStat(size: Int, mode: Int = 0, lastModified: Int = 0) {
        respond(Const.Message.LSTAT_V1)
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

    suspend fun respondDoneDone() {
        respond(Const.Message.DONEDONE)
    }

    suspend fun respondFailFail() {
        respond(Const.Message.FAILFAIL)
    }

    suspend fun respondList(size: Int, mode: Int = 0, lastModified: Int = 0, name: String) {
        respond(Const.Message.DENT_V1)
        writeIntLittleEndian(mode)
        writeIntLittleEndian(size)
        writeIntLittleEndian(lastModified)
        writeIntLittleEndian(name.length)
        writeStringUtf8(name)
    }

    suspend fun respondStringV1(message: String) {
        val lengthString = message.length.toString(16)
        val prepend = 4 - lengthString.length
        assert(prepend >= 0)
        var size = ""
        for (i in 0 until prepend) {
            size += "0"
        }
        size += lengthString
        writeFully(size.toByteArray(Const.DEFAULT_TRANSPORT_ENCODING))
        writeFully(message.toByteArray(Const.DEFAULT_TRANSPORT_ENCODING))
    }

    suspend fun respondStringRaw(message: String) {
        respond(message.toByteArray(Const.DEFAULT_TRANSPORT_ENCODING))
    }

    suspend fun respondFail(message: String) {
        respond(Const.Message.FAIL)
        writeIntLittleEndian(message.length)
        respondStringRaw(message)
    }
}
