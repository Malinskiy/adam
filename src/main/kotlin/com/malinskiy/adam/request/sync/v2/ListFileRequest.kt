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

package com.malinskiy.adam.request.sync.v2

import com.malinskiy.adam.Const
import com.malinskiy.adam.annotation.Features
import com.malinskiy.adam.extension.toInt
import com.malinskiy.adam.extension.toLong
import com.malinskiy.adam.extension.toUInt
import com.malinskiy.adam.extension.toULong
import com.malinskiy.adam.request.ComplexRequest
import com.malinskiy.adam.request.Feature
import com.malinskiy.adam.request.ValidationResponse
import com.malinskiy.adam.request.sync.model.FileEntryV2
import com.malinskiy.adam.transport.AndroidReadChannel
import com.malinskiy.adam.transport.AndroidWriteChannel
import java.time.Instant

@Features(Feature.LS_V2)
class ListFileRequest(
    private val remotePath: String
) : ComplexRequest<List<FileEntryV2>>() {

    override fun validate(): ValidationResponse {
        return if (remotePath.length > Const.MAX_REMOTE_PATH_LENGTH) {
            ValidationResponse(false, "Remote path should be less that ${Const.MAX_REMOTE_PATH_LENGTH} bytes")
        } else {
            ValidationResponse.Success
        }
    }

    override suspend fun readElement(readChannel: AndroidReadChannel, writeChannel: AndroidWriteChannel): List<FileEntryV2> {
        writeChannel.writeSyncRequest(Const.Message.LIST_V2, remotePath)

        val stringBytes = ByteArray(Const.MAX_REMOTE_PATH_LENGTH)

        val bytes = ByteArray(72)
        val result = mutableListOf<FileEntryV2>()
        loop@ while (true) {
            readChannel.readFully(bytes, 0, 4)
            when {
                bytes.copyOfRange(0, 4).contentEquals(Const.Message.DENT_V2) -> {
                    readChannel.readFully(bytes, 0, 72)
                    val nameLength = bytes.copyOfRange(68, 72).toInt()
                    readChannel.readFully(stringBytes, 0, nameLength)
                    result.add(
                        FileEntryV2(
                            error = bytes.copyOfRange(0, 4).toUInt(),
                            dev = bytes.copyOfRange(4, 12).toULong(),
                            ino = bytes.copyOfRange(12, 20).toULong(),
                            mode = bytes.copyOfRange(20, 24).toUInt(),
                            nlink = bytes.copyOfRange(24, 28).toUInt(),
                            uid = bytes.copyOfRange(28, 32).toUInt(),
                            gid = bytes.copyOfRange(32, 36).toUInt(),
                            size = bytes.copyOfRange(36, 44).toULong(),
                            atime = Instant.ofEpochSecond(bytes.copyOfRange(44, 52).toLong()),
                            mtime = Instant.ofEpochSecond(bytes.copyOfRange(52, 60).toLong()),
                            ctime = Instant.ofEpochSecond(bytes.copyOfRange(60, 68).toLong()),
                            name = String(stringBytes, 0, nameLength, Const.DEFAULT_TRANSPORT_ENCODING)
                        )
                    )
                }
                bytes.copyOfRange(0, 4).contentEquals(Const.Message.DONE) -> break@loop
                else -> break@loop
            }
        }

        return result
    }

    override fun serialize() = createBaseRequest("sync:")
}
