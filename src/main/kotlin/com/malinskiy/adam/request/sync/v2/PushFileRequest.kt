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
import com.malinskiy.adam.request.sync.base.BasePushFileRequest
import com.malinskiy.adam.transport.AndroidReadChannel
import com.malinskiy.adam.transport.AndroidWriteChannel
import kotlinx.coroutines.Dispatchers
import java.io.File
import kotlin.coroutines.CoroutineContext

/**
 * @param dryRun if true, requires SENDRECV_V2_DRY_RUN_SEND
 */
@Features(Feature.SENDRECV_V2, Feature.SENDRECV_V2_DRY_RUN_SEND)
class PushFileRequest(
    local: File,
    remotePath: String,
    val supportedFeatures: List<Feature>,
    mode: String = "0777",
    val dryRun: Boolean = false,
    coroutineContext: CoroutineContext = Dispatchers.IO
) : BasePushFileRequest(local, remotePath, mode, coroutineContext) {
    /**
     * We don't have support for any compression, so the only value is NONE
     */
    private val compressionType = CompressionType.NONE

    override suspend fun handshake(readChannel: AndroidReadChannel, writeChannel: AndroidWriteChannel) {
        super.handshake(readChannel, writeChannel)
        val additionalFlags = if (dryRun) {
            (DRY_RUN_FLAG or compressionType.toFlag().toLong()).toInt()
        } else {
            compressionType.toFlag()
        }
        writeChannel.writeSyncV2Request(Const.Message.SEND_V2, remotePath, additionalFlags, modeValue)
    }

    override fun validate(): ValidationResponse {
        val response = super.validate()
        return if (!response.success) {
            response
        } else if (!supportedFeatures.contains(Feature.SENDRECV_V2)) {
            ValidationResponse(false, ValidationResponse.missingFeature(Feature.SENDRECV_V2))
        } else if (dryRun && !supportedFeatures.contains(Feature.SENDRECV_V2_DRY_RUN_SEND)) {
            ValidationResponse(false, ValidationResponse.missingFeature(Feature.SENDRECV_V2_DRY_RUN_SEND))
        } else {
            ValidationResponse.Success
        }
    }

    companion object {
        val DRY_RUN_FLAG = 0x8000_0000
    }
}
