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
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import com.malinskiy.adam.AndroidDebugBridgeClient
import com.malinskiy.adam.request.forwarding.LocalTcpPortSpec
import com.malinskiy.adam.request.forwarding.LocalUnixSocketPortSpec
import com.malinskiy.adam.request.forwarding.RemoteAbstractPortSpec
import com.malinskiy.adam.request.forwarding.RemoteFilesystemPortSpec
import com.malinskiy.adam.request.forwarding.RemoteReservedPortSpec
import com.malinskiy.adam.request.forwarding.RemoteTcpPortSpec
import com.malinskiy.adam.server.junit4.AdbServerRule
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class ListReversePortForwardsRequestTest {
    @get:Rule
    val server = AdbServerRule()
    val client: AndroidDebugBridgeClient
        get() = server.client

    @Test
    fun testReturnsProperContent() {
        runBlocking {
            server.session {
                expectCmd { "host:transport:xx" }.accept()
                expectCmd { "reverse:list-forward" }.accept()

                respondListPortForwards(
                    """
                    xx tcp:80 tcp:80
                    xx localabstract:namedsocket local:/tmp/socket
                    xx localreserved:namedsocket local:/tmp/socket
                    xx localfilesystem:namedsocket local:/tmp/socket
                    
                """.trimIndent()
                )
            }

            val output = client.execute(ListReversePortForwardsRequest(), serial = "xx")
            assertThat(output).containsExactly(
                ReversePortForwardingRule("xx", RemoteTcpPortSpec(80), LocalTcpPortSpec(80)),
                ReversePortForwardingRule("xx", RemoteAbstractPortSpec("namedsocket"), LocalUnixSocketPortSpec("/tmp/socket")),
                ReversePortForwardingRule("xx", RemoteReservedPortSpec("namedsocket"), LocalUnixSocketPortSpec("/tmp/socket")),
                ReversePortForwardingRule("xx", RemoteFilesystemPortSpec("namedsocket"), LocalUnixSocketPortSpec("/tmp/socket"))
            )

            assertThat(output.first().serial).isEqualTo("xx")
            assertThat(output.first().remoteSpec).isEqualTo(LocalTcpPortSpec(80))
            assertThat(output.first().localSpec).isEqualTo(RemoteTcpPortSpec(80))
        }
    }
}
