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
import com.malinskiy.adam.extension.writeSyncRequest
import com.malinskiy.adam.request.ComplexRequest
import com.malinskiy.adam.request.Feature
import com.malinskiy.adam.request.ValidationResponse
import com.malinskiy.adam.request.sync.model.FileEntryV2
import com.malinskiy.adam.transport.Socket
import com.malinskiy.adam.transport.withDefaultBuffer
import java.time.Instant

@Features(Feature.LS_V2)
class ListFileRequest(
    private val remotePath: String,
    private val supportedFeatures: List<Feature>
) : ComplexRequest<List<FileEntryV2>>() {

    override fun validate(): ValidationResponse {
        val response = super.validate()
        return if (!response.success) {
            response
        } else if (remotePath.length > Const.MAX_REMOTE_PATH_LENGTH) {
            ValidationResponse(false, ValidationResponse.pathShouldNotBeLong())
        } else if (!supportedFeatures.contains(Feature.LS_V2)) {
            ValidationResponse(false, ValidationResponse.missingFeature(Feature.LS_V2))
        } else {
            ValidationResponse.Success
        }
    }

    override suspend fun readElement(socket: Socket): List<FileEntryV2> {
        socket.writeSyncRequest(Const.Message.LIST_V2, remotePath)

        withDefaultBuffer {
            val data = array()
//            val bytes = ByteArray(72)
            val result = mutableListOf<FileEntryV2>()
            loop@ while (true) {
                socket.readFully(data, 0, 4)
                when {
                    data.copyOfRange(0, 4).contentEquals(Const.Message.DENT_V2) -> {
                        socket.readFully(data, 0, 72)
                        val nameLength = data.copyOfRange(68, 72).toInt()
                        val error = data.copyOfRange(0, 4).toUInt()
                        val dev = data.copyOfRange(4, 12).toULong()
                        val ino = data.copyOfRange(12, 20).toULong()
                        val mode = data.copyOfRange(20, 24).toUInt()
                        val nlink = data.copyOfRange(24, 28).toUInt()
                        val uid = data.copyOfRange(28, 32).toUInt()
                        val gid = data.copyOfRange(32, 36).toUInt()
                        val size = data.copyOfRange(36, 44).toULong()
                        val atime = Instant.ofEpochSecond(data.copyOfRange(44, 52).toLong())
                        val mtime = Instant.ofEpochSecond(data.copyOfRange(52, 60).toLong())
                        val ctime = Instant.ofEpochSecond(data.copyOfRange(60, 68).toLong())

                        socket.readFully(data, 0, nameLength)
                        result.add(
                            FileEntryV2(
                                error = error,
                                dev = dev,
                                ino = ino,
                                mode = mode,
                                nlink = nlink,
                                uid = uid,
                                gid = gid,
                                size = size,
                                atime = atime,
                                mtime = mtime,
                                ctime = ctime,
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
