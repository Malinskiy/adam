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

package com.malinskiy.adam.request.reverse

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.malinskiy.adam.Const
import com.malinskiy.adam.exception.RequestRejectedException
import com.malinskiy.adam.request.forwarding.LocalTcpPortSpec
import com.malinskiy.adam.request.forwarding.PortForwardingMode
import com.malinskiy.adam.request.forwarding.RemoteTcpPortSpec
import com.malinskiy.adam.server.stub.AndroidDebugBridgeServer
import kotlinx.coroutines.runBlocking
import org.junit.Test

class ReversePortForwardRequestTest {
    @Test
    fun testSerializeDefault() {
        val bytes = ReversePortForwardRequest(RemoteTcpPortSpec(80), LocalTcpPortSpec(80)).serialize()

        assertThat(String(bytes, Const.DEFAULT_TRANSPORT_ENCODING))
            .isEqualTo("001Dreverse:forward:tcp:80;tcp:80")
    }

    @Test
    fun testSerializeNoDefault() {
        val bytes = ReversePortForwardRequest(
            RemoteTcpPortSpec(80),
            LocalTcpPortSpec(80),
            mode = PortForwardingMode.NO_REBIND
        ).serialize()

        assertThat(String(bytes, Const.DEFAULT_TRANSPORT_ENCODING))
            .isEqualTo("0026reverse:forward:norebind:tcp:80;tcp:80")
    }

    @Test
    fun testRead() {
        runBlocking {
            val server = AndroidDebugBridgeServer()

            val client = server.startAndListen { input, output ->
                val hostCmd = input.receiveCommand()
                assertThat(hostCmd).isEqualTo("host:transport:serial")
                output.respond(Const.Message.OKAY)

                val forwardCmd = input.receiveCommand()
                assertThat(forwardCmd).isEqualTo("reverse:forward:tcp:8080;tcp:0")
                output.respond(Const.Message.OKAY)

                output.respond(Const.Message.OKAY)
                output.respondStringV1("7070")
            }

            val output = client.execute(ReversePortForwardRequest(RemoteTcpPortSpec(8080), LocalTcpPortSpec(0)), "serial")
            assertThat(output).isEqualTo(7070)

            server.dispose()
        }
    }

    @Test(expected = RequestRejectedException::class)
    fun testReadFailure() {
        runBlocking {
            val server = AndroidDebugBridgeServer()

            val client = server.startAndListen { input, output ->
                val hostCmd = input.receiveCommand()
                assertThat(hostCmd).isEqualTo("host:transport:serial")
                output.respond(Const.Message.OKAY)

                val forwardCmd = input.receiveCommand()
                assertThat(forwardCmd).isEqualTo("reverse:forward:tcp:8080;tcp:0")

                output.respond(Const.Message.FAIL)
                output.respondStringV1("7070")
            }

            val output = client.execute(ReversePortForwardRequest(RemoteTcpPortSpec(8080), LocalTcpPortSpec(0)), "serial")
            server.dispose()
        }
    }
}
