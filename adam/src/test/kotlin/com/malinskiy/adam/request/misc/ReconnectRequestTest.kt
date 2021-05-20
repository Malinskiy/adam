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

package com.malinskiy.adam.request.misc

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.malinskiy.adam.extension.toRequestString
import com.malinskiy.adam.request.HostTarget
import com.malinskiy.adam.request.SerialTarget
import com.malinskiy.adam.server.AndroidDebugBridgeServer
import io.ktor.utils.io.close
import kotlinx.coroutines.runBlocking
import org.junit.Test

class ReconnectRequestTest {
    @Test
    fun testSerialize() {
        val bytes = ReconnectRequest().serialize()
        assertThat(bytes.toRequestString()).isEqualTo("0009reconnect")
    }

    @Test
    fun testSerializeOffline() {
        val bytes = ReconnectRequest(reconnectTarget = Offline, target = HostTarget).serialize()
        assertThat(bytes.toRequestString()).isEqualTo("0016host:reconnect-offline")
    }

    @Test
    fun testSerializeDevice() {
        val bytes = ReconnectRequest(reconnectTarget = Device, target = SerialTarget("serial")).serialize()
        assertThat(bytes.toRequestString()).isEqualTo("001Chost-serial:serial:reconnect")
    }

    @Test
    fun testResponse() = runBlocking {
        val server = AndroidDebugBridgeServer()

        val client = server.startAndListen { input, output ->
            val cmd = input.receiveCommand()
            assertThat(cmd).isEqualTo("host:reconnect-offline")
            output.respondOkay()

            output.respondStringV1("reconnecting emulator-5554 [offline]")
            output.close()
        }

        val output = client.execute(ReconnectRequest(reconnectTarget = Offline, target = HostTarget))
        assertThat(output).isEqualTo("reconnecting emulator-5554 [offline]")

        server.dispose()
    }

    @Test
    fun testResponseForSingleTarget() = runBlocking {
        val server = AndroidDebugBridgeServer()

        val client = server.startAndListen { input, output ->
            val cmd = input.receiveCommand()
            assertThat(cmd).isEqualTo("host-serial:serial:reconnect")
            output.respondOkay()
            output.respondStringRaw("done")
            output.close()
        }

        val output = client.execute(ReconnectRequest(reconnectTarget = Device, target = SerialTarget("serial")))
        assertThat(output).isEqualTo("done")

        server.dispose()
    }
}
