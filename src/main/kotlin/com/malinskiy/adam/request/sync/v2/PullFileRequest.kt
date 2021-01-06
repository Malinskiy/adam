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
import com.malinskiy.adam.request.Feature
import com.malinskiy.adam.request.ValidationResponse
import com.malinskiy.adam.request.sync.base.BasePullFileRequest
import com.malinskiy.adam.transport.AndroidReadChannel
import com.malinskiy.adam.transport.AndroidWriteChannel
import kotlinx.coroutines.Dispatchers
import java.io.File
import kotlin.coroutines.CoroutineContext

@Features(Feature.SENDRECV_V2)
class PullFileRequest(
    private val remotePath: String,
    local: File,
    private val supportedFeatures: List<Feature>,
    size: Long? = null,
    coroutineContext: CoroutineContext = Dispatchers.IO
) : BasePullFileRequest(remotePath, local, size, coroutineContext) {
    /**
     * We don't have support for any compression, so the only value is NONE
     */
    private val compressionType = CompressionType.NONE

    override suspend fun handshake(readChannel: AndroidReadChannel, writeChannel: AndroidWriteChannel) {
        super.handshake(readChannel, writeChannel)
        writeChannel.writeSyncV2Request(Const.Message.RECV_V2, remotePath, compressionType.toFlag())
    }

    override fun validate(): ValidationResponse {
        val response = super.validate()
        return if (!response.success) {
            response
        } else if (!supportedFeatures.contains(Feature.SENDRECV_V2)) {
            ValidationResponse(false, ValidationResponse.missingFeature(Feature.SENDRECV_V2))
        } else {
            ValidationResponse.Success
        }
    }
}
