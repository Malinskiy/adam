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
import assertk.assertions.isEqualTo
import com.malinskiy.adam.Const
import com.malinskiy.adam.server.AndroidDebugBridgeServer
import io.ktor.utils.io.*
import kotlinx.coroutines.runBlocking
import org.junit.Test

class ListDevicesRequestTest {
    @Test
    fun testReturnsProperContent() {
        runBlocking {
            val server = AndroidDebugBridgeServer()

            val client = server.startAndListen { input, output ->
                val transportCmd = input.receiveCommand()
                assertThat(transportCmd).isEqualTo("host:devices")
                output.respond(Const.Message.OKAY)

                val response = ("00FA" +
                        "emulator-5554\toffline\n" +
                        "emulator-5556\tbootloader\n" +
                        "emulator-5558\tdevice\n" +
                        "emulator-5560\thost\n" +
                        "emulator-5562\trecovery\n" +
                        "emulator-5564\trescue\n" +
                        "emulator-5566\tsideload\n" +
                        "emulator-5568\tunauthorized\n" +
                        "emulator-5570\tauthorizing\n" +
                        "emulator-5572\tconnecting\n" +
                        "emulator-5574\twtf\n"
                        ).toByteArray(Const.DEFAULT_TRANSPORT_ENCODING)
                output.writeFully(response, 0, response.size)
                output.close()
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

            server.dispose()
        }
    }
}
