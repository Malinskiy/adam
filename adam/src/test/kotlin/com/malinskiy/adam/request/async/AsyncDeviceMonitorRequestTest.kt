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
import assertk.assertions.isEqualTo
import com.malinskiy.adam.Const
import com.malinskiy.adam.request.device.AsyncDeviceMonitorRequest
import com.malinskiy.adam.request.device.Device
import com.malinskiy.adam.request.device.DeviceState
import com.malinskiy.adam.server.stub.AndroidDebugBridgeServer
import kotlinx.coroutines.runBlocking
import org.junit.Test

class AsyncDeviceMonitorRequestTest {
    @Test
    fun testReturnsProperContent() {
        runBlocking {
            val server = AndroidDebugBridgeServer()

            val client = server.startAndListen { input, output ->
                val transportCmd = input.receiveCommand()
                assertThat(transportCmd).isEqualTo("host:track-devices")
                output.respond(Const.Message.OKAY)

                var response = ("0016emulator-5554\toffline\n").toByteArray(Const.DEFAULT_TRANSPORT_ENCODING)
                output.writeFully(response, 0, response.size)
                response = ("0015emulator-5554\tdevice\n").toByteArray(Const.DEFAULT_TRANSPORT_ENCODING)
                output.writeFully(response, 0, response.size)
            }

            val updates = client.execute(AsyncDeviceMonitorRequest(), scope = this)
            var update = updates.receiveOrNull()
            assertThat(update!!).containsExactly(Device("emulator-5554", DeviceState.OFFLINE))
            update = updates.receiveOrNull()
            assertThat(update!!).containsExactly(Device("emulator-5554", DeviceState.DEVICE))
            updates.cancel()

            server.dispose()
        }
    }
}
