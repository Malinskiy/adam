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

package com.malinskiy.adam.request.shell.v1

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.malinskiy.adam.AndroidDebugBridgeClient
import com.malinskiy.adam.server.junit4.AdbServerRule
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test


class ShellCommandRequestTest {
    @get:Rule
    val server = AdbServerRule()
    val client: AndroidDebugBridgeClient
        get() = server.client

    @Test
    fun testReturnsProperContent() {
        runBlocking {
            server.session {
                expectCmd { "host:transport:serial" }.accept()
                expectCmd { "shell:xx;echo x$?" }.accept()
                respondShellV1("something-somethingx1")
            }

            val output = client.execute(ShellCommandRequest("xx"), serial = "serial")
            assertThat(output.output).isEqualTo("something-something")
            assertThat(output.exitCode).isEqualTo(1)
        }
    }

    @Test
    fun testReturnsNonStrippedStdout() {
        runBlocking {
            server.session {
                expectCmd { "host:transport:serial" }.accept()
                expectCmd { "shell:xx;echo x$?" }.accept()
                respondShellV1("something-something\nx1")
            }

            val output = client.execute(ShellCommandRequest("xx"), serial = "serial")
            assertThat(output.output).isEqualTo("something-something\n")
            assertThat(output.exitCode).isEqualTo(1)
        }
    }
}
