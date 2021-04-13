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
import com.malinskiy.adam.Const
import com.malinskiy.adam.request.forwarding.*
import com.malinskiy.adam.server.AndroidDebugBridgeServer
import io.ktor.utils.io.close
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Test

class ListReversePortForwardsRequestTest {
    @Test
    fun testReturnsProperContent() {
        runBlocking {
            val server = AndroidDebugBridgeServer()

            val client = server.startAndListen { input, output ->
                val transportCmd = input.receiveCommand()
                assertThat(transportCmd).isEqualTo("host:transport:xx")
                output.respond(Const.Message.OKAY)

                val reverseCmd = input.receiveCommand()
                assertThat(reverseCmd).isEqualTo("reverse:list-forward")
                output.respond(Const.Message.OKAY)
                output.respondStringV1(
                    """
                    xx tcp:80 tcp:80
                    xx localabstract:namedsocket local:/tmp/socket
                    xx localreserved:namedsocket local:/tmp/socket
                    xx localfilesystem:namedsocket local:/tmp/socket
                    
                """.trimIndent()
                )
                output.close()
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

            server.dispose()
        }
    }
}
