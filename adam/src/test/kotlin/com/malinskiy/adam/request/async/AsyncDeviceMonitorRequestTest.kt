/*
 * Copyright (C) 2020 Anton Malinskiy
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

package com.malinskiy.adam.request.async

import assertk.assertThat
import assertk.assertions.containsExactly
import com.malinskiy.adam.AndroidDebugBridgeClient
import com.malinskiy.adam.request.device.AsyncDeviceMonitorRequest
import com.malinskiy.adam.request.device.Device
import com.malinskiy.adam.request.device.DeviceState
import com.malinskiy.adam.server.junit4.AdbServerRule
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class AsyncDeviceMonitorRequestTest {
    @get:Rule
    val server = AdbServerRule()
    val client: AndroidDebugBridgeClient
        get() = server.client

    @Test
    fun testReturnsProperContent() {
        runBlocking {
            server.session {
                expectCmd { "host:track-devices" }.accept()
                respondAsyncDeviceMonitor("emulator-5554", "offline")
                respondAsyncDeviceMonitor("emulator-5554", "device")
            }

            val updates = client.execute(AsyncDeviceMonitorRequest(), scope = this)
            var update = updates.receiveOrNull()
            assertThat(update!!).containsExactly(Device("emulator-5554", DeviceState.OFFLINE))
            update = updates.receiveOrNull()
            assertThat(update!!).containsExactly(Device("emulator-5554", DeviceState.DEVICE))
            updates.cancel()
        }
    }
}
