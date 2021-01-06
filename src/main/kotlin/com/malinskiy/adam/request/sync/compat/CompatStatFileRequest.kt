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

package com.malinskiy.adam.request.sync.compat

import com.malinskiy.adam.AndroidDebugBridgeClient
import com.malinskiy.adam.request.Feature
import com.malinskiy.adam.request.MultiRequest
import com.malinskiy.adam.request.sync.model.FileEntry
import com.malinskiy.adam.request.sync.v1.StatFileRequest
import com.malinskiy.adam.request.sync.v2.StatFileRequest as StatV2FileRequest

class CompatStatFileRequest(
    private val source: String,
    private val supportedFeatures: List<Feature>
) : MultiRequest<FileEntry>() {
    override suspend fun execute(androidDebugBridgeClient: AndroidDebugBridgeClient, serial: String?): FileEntry {
        return when {
            supportedFeatures.contains(Feature.STAT_V2) -> androidDebugBridgeClient.execute(
                StatV2FileRequest(source, supportedFeatures),
                serial
            )
            else -> androidDebugBridgeClient.execute(StatFileRequest(source), serial)
        }
    }
}
