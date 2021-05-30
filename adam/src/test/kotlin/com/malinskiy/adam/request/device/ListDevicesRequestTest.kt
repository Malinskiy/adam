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

package com.malinskiy.adam.request.device

import assertk.assertThat
import assertk.assertions.containsExactly
import com.malinskiy.adam.AndroidDebugBridgeClient
import com.malinskiy.adam.server.junit4.AdbServerRule
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class ListDevicesRequestTest {
    @get:Rule
    val server = AdbServerRule()
    val client: AndroidDebugBridgeClient
        get() = server.client

    @Test
    fun testReturnsProperContent() {
        runBlocking {
            server.session {
                expectCmd { "host:devices" }.accept()
                respondListDevices(
                    mapOf(
                        "emulator-5554" to "offline",
                        "emulator-5556" to "bootloader",
                        "emulator-5558" to "device",
                        "emulator-5560" to "host",
                        "emulator-5562" to "recovery",
                        "emulator-5564" to "rescue",
                        "emulator-5566" to "sideload",
                        "emulator-5568" to "unauthorized",
                        "emulator-5570" to "authorizing",
                        "emulator-5572" to "connecting",
                        "emulator-5574" to "wtf",
                    )
                )
            }

            val version = client.execute(ListDevicesRequest())
            assertThat(version).containsExactly(
                Device("emulator-5554", DeviceState.OFFLINE),
                Device("emulator-5556", DeviceState.BOOTLOADER),
                Device("emulator-5558", DeviceState.DEVICE),
                Device("emulator-5560", DeviceState.HOST),
                Device("emulator-5562", DeviceState.RECOVERY),
                Device("emulator-5564", DeviceState.RESCUE),
                Device("emulator-5566", DeviceState.SIDELOAD),
                Device("emulator-5568", DeviceState.UNAUTHORIZED),
                Device("emulator-5570", DeviceState.AUTHORIZING),
                Device("emulator-5572", DeviceState.CONNECTING),
                Device("emulator-5574", DeviceState.UNKNOWN)
            )
        }
    }
}
