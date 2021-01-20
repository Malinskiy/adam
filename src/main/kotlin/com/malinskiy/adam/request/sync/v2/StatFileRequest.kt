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
import com.malinskiy.adam.exception.UnsupportedSyncProtocolException
import com.malinskiy.adam.extension.toLong
import com.malinskiy.adam.extension.toUInt
import com.malinskiy.adam.extension.toULong
import com.malinskiy.adam.extension.writeSyncRequest
import com.malinskiy.adam.request.ComplexRequest
import com.malinskiy.adam.request.Feature
import com.malinskiy.adam.request.ValidationResponse
import com.malinskiy.adam.request.sync.model.FileEntryV2
import com.malinskiy.adam.transport.Socket
import java.time.Instant

@Features(Feature.STAT_V2)
class StatFileRequest(
    private val remotePath: String,
    private val supportedFeatures: List<Feature>
) : ComplexRequest<FileEntryV2>() {
    override suspend fun readElement(socket: Socket): FileEntryV2 {
        socket.writeSyncRequest(Const.Message.LSTAT_V2, remotePath)

        val bytes = ByteArray(72)
        socket.readFully(bytes, 0, 72)

        if (!bytes.copyOfRange(0, 4).contentEquals(Const.Message.LSTAT_V2)) throw UnsupportedSyncProtocolException()

        return FileEntryV2(
            error = bytes.copyOfRange(4, 8).toUInt(),
            dev = bytes.copyOfRange(8, 16).toULong(),
            ino = bytes.copyOfRange(16, 24).toULong(),
            mode = bytes.copyOfRange(24, 28).toUInt(),
            nlink = bytes.copyOfRange(28, 32).toUInt(),
            uid = bytes.copyOfRange(32, 36).toUInt(),
            gid = bytes.copyOfRange(36, 40).toUInt(),
            size = bytes.copyOfRange(40, 48).toULong(),
            atime = Instant.ofEpochSecond(bytes.copyOfRange(48, 56).toLong()),
            mtime = Instant.ofEpochSecond(bytes.copyOfRange(56, 64).toLong()),
            ctime = Instant.ofEpochSecond(bytes.copyOfRange(64, 72).toLong())
        )
    }

    override fun validate(): ValidationResponse {
        val response = super.validate()
        return if (!response.success) {
            response
        } else if (remotePath.length > Const.MAX_REMOTE_PATH_LENGTH) {
            ValidationResponse(false, ValidationResponse.pathShouldNotBeLong())
        } else if (!supportedFeatures.contains(Feature.STAT_V2)) {
            ValidationResponse(false, ValidationResponse.missingFeature(Feature.STAT_V2))
        } else {
            ValidationResponse.Success
        }
    }

    override fun serialize() = createBaseRequest("sync:")
}

