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

package com.malinskiy.adam.request.sync.v1

import com.malinskiy.adam.Const
import com.malinskiy.adam.exception.UnsupportedSyncProtocolException
import com.malinskiy.adam.extension.toByteArray
import com.malinskiy.adam.extension.toInt
import com.malinskiy.adam.request.ComplexRequest
import com.malinskiy.adam.transport.AndroidReadChannel
import com.malinskiy.adam.transport.AndroidWriteChannel
import java.time.Instant

class StatFileRequest(
    private val remotePath: String
) : ComplexRequest<FileStats>() {
    override suspend fun readElement(readChannel: AndroidReadChannel, writeChannel: AndroidWriteChannel): FileStats {
        val bytes = ByteArray(16)

        val type = Const.Message.LSTAT_V1

        val path = remotePath.toByteArray(Const.DEFAULT_TRANSPORT_ENCODING)
        val size = path.size.toByteArray().reversedArray()

        val cmd = ByteArray(8 + path.size)

        type.copyInto(cmd)
        size.copyInto(cmd, 4)
        path.copyInto(cmd, 8)

        writeChannel.write(cmd)
        readChannel.readFully(bytes, 0, 16)

        if (!bytes.copyOfRange(0, 4).contentEquals(Const.Message.LSTAT_V1)) throw UnsupportedSyncProtocolException()

        return FileStats(
            mode = bytes.copyOfRange(4, 8).toInt(),
            size = bytes.copyOfRange(8, 12).toInt(),
            lastModified = Instant.ofEpochSecond(bytes.copyOfRange(12, 16).toInt().toLong())
        )
    }

    override fun serialize() = createBaseRequest("sync:")
}

