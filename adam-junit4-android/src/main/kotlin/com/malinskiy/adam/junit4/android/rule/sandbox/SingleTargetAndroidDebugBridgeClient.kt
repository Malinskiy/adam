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

package com.malinskiy.adam.junit4.android.rule.sandbox

import com.malinskiy.adam.AndroidDebugBridgeClient
import com.malinskiy.adam.request.AsyncChannelRequest
import com.malinskiy.adam.request.ComplexRequest
import com.malinskiy.adam.request.MultiRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel

class SingleTargetAndroidDebugBridgeClient(private val client: AndroidDebugBridgeClient, private val serial: String) {

    suspend fun <T : Any?> execute(request: ComplexRequest<T>): T = client.execute(request, serial)

    fun <T : Any?, I : Any?> execute(request: AsyncChannelRequest<T, I>, scope: CoroutineScope): ReceiveChannel<T> =
        client.execute(request, scope, serial)

    suspend fun <T> execute(request: MultiRequest<T>): T = client.execute(request, serial)
}