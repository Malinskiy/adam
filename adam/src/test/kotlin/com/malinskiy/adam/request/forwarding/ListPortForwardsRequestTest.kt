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

package com.malinskiy.adam.request.forwarding

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import com.malinskiy.adam.AndroidDebugBridgeClient
import com.malinskiy.adam.server.junit4.AdbServerRule
import io.ktor.utils.io.discard
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class ListPortForwardsRequestTest {
    @get:Rule
    val server = AdbServerRule()
    val client: AndroidDebugBridgeClient
        get() = server.client

    @Test
    fun testReturnsProperContent() {
        runBlocking {
            server.session {
                expectCmd { "host-serial:xx:list-forward" }.accept()

                respondListPortForwards(
                    """
                        xx tcp:80 tcp:80
                        xx local:/tmp/socket localabstract:namedsocket
                        xx local:/tmp/socket localreserved:namedsocket
                        xx local:/tmp/socket localfilesystem:namedsocket
                        xx local:/tmp/socket dev:/dev/chardev
                        xx local:/tmp/socket jdwp:1001
                        
                    """.trimIndent()
                )

                input.discard()
            }

            val output = client.execute(ListPortForwardsRequest("xx"))
            assertThat(output).containsExactly(
                PortForwardingRule("xx", LocalTcpPortSpec(80), RemoteTcpPortSpec(80)),
                PortForwardingRule("xx", LocalUnixSocketPortSpec("/tmp/socket"), RemoteAbstractPortSpec("namedsocket")),
                PortForwardingRule("xx", LocalUnixSocketPortSpec("/tmp/socket"), RemoteReservedPortSpec("namedsocket")),
                PortForwardingRule("xx", LocalUnixSocketPortSpec("/tmp/socket"), RemoteFilesystemPortSpec("namedsocket")),
                PortForwardingRule("xx", LocalUnixSocketPortSpec("/tmp/socket"), RemoteDevPortSpec("/dev/chardev")),
                PortForwardingRule("xx", LocalUnixSocketPortSpec("/tmp/socket"), JDWPPortSpec(1001))
            )

            assertThat(output.first().serial).isEqualTo("xx")
            assertThat(output.first().localSpec).isEqualTo(LocalTcpPortSpec(80))
            assertThat(output.first().remoteSpec).isEqualTo(RemoteTcpPortSpec(80))
        }
    }
}
