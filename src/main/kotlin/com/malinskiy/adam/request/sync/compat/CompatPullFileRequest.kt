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
import com.malinskiy.adam.request.sync.v1.PullFileRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ReceiveChannel
import java.io.File
import kotlin.coroutines.CoroutineContext
import com.malinskiy.adam.request.sync.v2.PullFileRequest as PullV2FileRequest

class CompatPullFileRequest(
    private val source: String,
    private val realDestination: File,
    private val size: Long? = null,
    private val supportedFeatures: List<Feature>,
    private val coroutineScope: CoroutineScope,
    private val coroutineContext: CoroutineContext = Dispatchers.IO
) : MultiRequest<ReceiveChannel<Double>>() {
    override suspend fun execute(androidDebugBridgeClient: AndroidDebugBridgeClient, serial: String?): ReceiveChannel<Double> {
        return when {
            supportedFeatures.contains(Feature.SENDRECV_V2) -> {
                androidDebugBridgeClient.execute(
                    PullV2FileRequest(source, realDestination, supportedFeatures, size, coroutineContext),
                    coroutineScope,
                    serial
                )
            }
            else -> {
                androidDebugBridgeClient.execute(
                    PullFileRequest(source, realDestination, size, coroutineContext),
                    coroutineScope,
                    serial
                )
            }
        }
    }
}
