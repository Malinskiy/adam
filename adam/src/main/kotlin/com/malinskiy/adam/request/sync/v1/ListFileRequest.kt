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
import com.malinskiy.adam.extension.writeSyncRequest
import com.malinskiy.adam.request.ComplexRequest
import com.malinskiy.adam.request.ValidationResponse
import com.malinskiy.adam.request.sync.model.FileEntryV1
import com.malinskiy.adam.transport.Socket
import com.malinskiy.adam.transport.withDefaultBuffer
import java.time.Instant

class ListFileRequest(
    private val remotePath: String
) : ComplexRequest<List<FileEntryV1>>() {

    override fun validate(): ValidationResponse {
        return if (remotePath.length > Const.MAX_REMOTE_PATH_LENGTH) {
            ValidationResponse(false, "Remote path should be less that ${Const.MAX_REMOTE_PATH_LENGTH} bytes")
        } else {
            ValidationResponse.Success
        }
    }

    override suspend fun readElement(socket: Socket): List<FileEntryV1> {
        socket.writeSyncRequest(Const.Message.LIST_V1, remotePath)

        withDefaultBuffer {
            val data = array()
            val result = mutableListOf<FileEntryV1>()
            loop@ while (true) {

                socket.readFully(data, 0, 4)
                when {
                    data.copyOfRange(0, 4).contentEquals(Const.Message.DENT_V1) -> {
                        socket.readFully(data, 0, 16)
                        val mode = data.copyOfRange(0, 4).toInt().toUInt()
                        val size = data.copyOfRange(4, 8).toInt().toUInt()
                        val mtime = Instant.ofEpochSecond(data.copyOfRange(8, 12).toInt().toLong())
                        val nameLength = data.copyOfRange(12, 16).toInt()
                        socket.readFully(data, 0, nameLength)

                        result.add(
                            FileEntryV1(
                                mode = mode,
                                size = size,
                                mtime = mtime,
                                name = String(data, 0, nameLength, Const.DEFAULT_TRANSPORT_ENCODING)
                            )
                        )
                    }
                    data.copyOfRange(0, 4).contentEquals(Const.Message.DONE) -> break@loop
                    else -> break@loop
                }
            }

            return result
        }
    }

    override fun serialize() = createBaseRequest("sync:")
}
