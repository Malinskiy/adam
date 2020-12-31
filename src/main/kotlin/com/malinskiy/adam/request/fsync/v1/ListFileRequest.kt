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

package com.malinskiy.adam.request.fsync.v1

import com.malinskiy.adam.Const
import com.malinskiy.adam.extension.toByteArray
import com.malinskiy.adam.extension.toInt
import com.malinskiy.adam.request.ComplexRequest
import com.malinskiy.adam.transport.AndroidReadChannel
import com.malinskiy.adam.transport.AndroidWriteChannel
import java.time.Instant

class ListFileRequest(
    private val remotePath: String
) : ComplexRequest<List<FileStats>>() {
    override suspend fun readElement(readChannel: AndroidReadChannel, writeChannel: AndroidWriteChannel): List<FileStats> {
        val bytes = ByteArray(16)
        val stringBytes = ByteArray(Const.MAX_REMOTE_PATH_LENGTH)

        val type = Const.Message.LIST_V1

        val path = remotePath.toByteArray(Const.DEFAULT_TRANSPORT_ENCODING)
        val size = path.size.toByteArray().reversedArray()

        val cmd = ByteArray(8 + path.size)

        type.copyInto(cmd)
        size.copyInto(cmd, 4)
        path.copyInto(cmd, 8)

        writeChannel.write(cmd)

        val result = mutableListOf<FileStats>()
        loop@ while (true) {
            readChannel.readFully(bytes, 0, 4)
            when {
                bytes.copyOfRange(0, 4).contentEquals(Const.Message.DENT_V1) -> {
                    readChannel.readFully(bytes, 0, 16)
                    val nameLength = bytes.copyOfRange(12, 16).toInt()
                    readChannel.readFully(stringBytes, 0, nameLength)

                    result.add(
                        FileStats(
                            mode = bytes.copyOfRange(0, 4).toInt(),
                            size = bytes.copyOfRange(4, 8).toInt(),
                            lastModified = Instant.ofEpochSecond(bytes.copyOfRange(8, 12).toInt().toLong()),
                            name = String(stringBytes, 0, nameLength, Const.FILENAME_ENCODING)

                        )
                    )
                }
                bytes.copyOfRange(0, 4).contentEquals(Const.Message.DONE) -> break@loop
            }
        }

        return result
    }

    override fun serialize() = createBaseRequest("sync:")
}