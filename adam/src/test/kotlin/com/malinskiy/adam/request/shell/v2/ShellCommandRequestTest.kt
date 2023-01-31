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

package com.malinskiy.adam.request.shell.v2

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.malinskiy.adam.AndroidDebugBridgeClient
import com.malinskiy.adam.exception.RequestValidationException
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
    fun testReturnsNonStrippedStdout() {
        runBlocking {
            server.session {
                expectCmd { "host:transport:serial" }.accept()
                expectCmd { "shell,v2,raw:echo foo; echo bar >&2; exit 17" }.accept()

                respondShellV2Stdout("fo")
                respondShellV2Stderr("ba")
                respondShellV2Stdout("o\n")
                respondShellV2Stderr("r\n")
                respondShellV2Exit(17)
            }


            val output = client.execute(ShellCommandRequest("echo foo; echo bar >&2; exit 17"), serial = "serial")
            assertThat(output.output).isEqualTo("foo\n")
            assertThat(output.errorOutput).isEqualTo("bar\n")
            assertThat(output.exitCode).isEqualTo(17)
        }
    }

    @Test(expected = RequestValidationException::class)
    fun testUnsupportedWINDOW_SIZE_CHANGE() {
        runUnsupportedTestMessage(MessageType.WINDOW_SIZE_CHANGE)
    }

    @Test(expected = RequestValidationException::class)
    fun testUnsupportedSTDIN() {
        runUnsupportedTestMessage(MessageType.STDIN)
    }

    @Test(expected = RequestValidationException::class)
    fun testUnsupportedINVALID() {
        runUnsupportedTestMessage(MessageType.INVALID)
    }

    @Test(expected = RequestValidationException::class)
    fun testUnsupportedCLOSE_STDIN() {
        runUnsupportedTestMessage(MessageType.CLOSE_STDIN)
    }

    private fun runUnsupportedTestMessage(messageType: MessageType) {
        runBlocking {
            server.session {
                expectCmd { "host:transport:serial" }.accept()
                expectCmd { "shell,v2,raw:echo foo; echo bar >&2; exit 17" }.accept()
                output.writeByte(messageType.toValue().toByte())
            }

            val output = client.execute(ShellCommandRequest("echo foo; echo bar >&2; exit 17"), serial = "serial")
            assertThat(output.stdout).isEqualTo("foo\n")
            assertThat(output.stderr).isEqualTo("bar\n")
            assertThat(output.exitCode).isEqualTo(17)
        }
    }
}
