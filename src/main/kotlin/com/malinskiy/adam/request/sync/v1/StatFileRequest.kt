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
import com.malinskiy.adam.extension.compatLimit
import com.malinskiy.adam.extension.toInt
import com.malinskiy.adam.extension.toUInt
import com.malinskiy.adam.extension.writeSyncRequest
import com.malinskiy.adam.request.ComplexRequest
import com.malinskiy.adam.request.ValidationResponse
import com.malinskiy.adam.request.sync.model.FileEntryV1
import com.malinskiy.adam.transport.Socket
import com.malinskiy.adam.transport.withDefaultBuffer
import java.time.Instant

class StatFileRequest(
    private val remotePath: String
) : ComplexRequest<FileEntryV1>() {
    override suspend fun readElement(socket: Socket): FileEntryV1 {
        socket.writeSyncRequest(Const.Message.LSTAT_V1, remotePath)

        withDefaultBuffer {
            compatLimit(16)
            socket.readFully(this)
            flip()

            val bytes = array()
            if (!bytes.copyOfRange(0, 4).contentEquals(Const.Message.LSTAT_V1)) throw UnsupportedSyncProtocolException()

            return FileEntryV1(
                mode = bytes.copyOfRange(4, 8).toUInt(),
                size = bytes.copyOfRange(8, 12).toUInt(),
                mtime = Instant.ofEpochSecond(bytes.copyOfRange(12, 16).toInt().toLong())
            )
        }
    }

    override fun validate(): ValidationResponse {
        return if (remotePath.length > Const.MAX_REMOTE_PATH_LENGTH) {
            ValidationResponse(false, "Remote path should be less that ${Const.MAX_REMOTE_PATH_LENGTH} bytes")
        } else {
            ValidationResponse.Success
        }
    }

    override fun serialize() = createBaseRequest("sync:")
}

