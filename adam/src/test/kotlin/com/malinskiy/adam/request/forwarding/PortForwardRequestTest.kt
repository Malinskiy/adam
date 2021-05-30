/*
 * Copyright (C) 2019 Anton Malinskiy
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

package com.malinskiy.adam.request.forwarding

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.malinskiy.adam.AndroidDebugBridgeClient
import com.malinskiy.adam.Const
import com.malinskiy.adam.exception.RequestRejectedException
import com.malinskiy.adam.server.junit4.AdbServerRule
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class PortForwardRequestTest {
    @get:Rule
    val server = AdbServerRule()
    val client: AndroidDebugBridgeClient
        get() = server.client

    @Test
    fun testSerializeDefault() {
        val bytes = PortForwardRequest(LocalTcpPortSpec(80), RemoteTcpPortSpec(80), "emulator-5554").serialize()

        assertThat(String(bytes, Const.DEFAULT_TRANSPORT_ENCODING))
            .isEqualTo("002Fhost-serial:emulator-5554:forward:tcp:80;tcp:80")
    }

    @Test
    fun testSerializeNoDefault() {
        val bytes = PortForwardRequest(
            LocalTcpPortSpec(80),
            RemoteTcpPortSpec(80),
            "emulator-5554",
            mode = PortForwardingMode.NO_REBIND
        ).serialize()

        assertThat(String(bytes, Const.DEFAULT_TRANSPORT_ENCODING))
            .isEqualTo("0038host-serial:emulator-5554:forward:norebind:tcp:80;tcp:80")
    }

    @Test
    fun testRead() {
        runBlocking {
            server.session {
                expectCmd { "host-serial:serial:forward:tcp:0;tcp:8080" }.accept()
                respondPortForward(true, 7070)
            }

            val output = client.execute(PortForwardRequest(LocalTcpPortSpec(0), RemoteTcpPortSpec(8080), "serial"))
            assertThat(output).isEqualTo(7070)
        }
    }

    @Test(expected = RequestRejectedException::class)
    fun testReadFailure() {
        runBlocking {
            server.session {
                expectCmd { "host-serial:serial:forward:tcp:0;tcp:8080" }.accept()
                respondPortForward(false)
            }

            client.execute(PortForwardRequest(LocalTcpPortSpec(0), RemoteTcpPortSpec(8080), "serial"))
        }
    }
}
