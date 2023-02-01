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
import com.malinskiy.adam.Const
import com.malinskiy.adam.server.junit4.AdbServerRule
import io.ktor.utils.io.discard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Rule
import org.junit.Test

class ChanneledShellCommandRequestTest {
    @get:Rule
    val server = AdbServerRule()
    val client: AndroidDebugBridgeClient
        get() = server.client

    @Test
    fun testReturnsProperContent() {
        runBlocking {
            server.session {
                expectCmd { "host:transport:serial" }.accept()
                expectCmd { "shell,v2,raw:echo foo; echo bar >&2; exit 17" }.accept()

                expectShellV2Stdin("cafebabe")
                expectShellV2StdinClose()

                respondShellV2Stdout("fo")
                respondShellV2Stderr("ba")
                respondShellV2Stdout("o\n")
                respondShellV2WindowSizeChange()
                respondShellV2Invalid()
                respondShellV2Stderr("r\n")
                respondShellV2Exit(17)

                input.discard()
            }

            val stdio = Channel<ShellCommandInputChunk>()
            val updates = client.execute(
                ChanneledShellCommandRequest("echo foo; echo bar >&2; exit 17", stdio),
                scope = this,
                serial = "serial"
            )
            val stdoutBuffer = StringBuffer()
            val stderrBuffer = StringBuffer()
            var exitCode = -1

            withContext(Dispatchers.IO) {
                stdio.send(ShellCommandInputChunk("cafebabe".toByteArray(Const.DEFAULT_TRANSPORT_ENCODING)))
                stdio.send(ShellCommandInputChunk(close = true))
            }

            for (msg in updates) {
                msg.stdout?.let { stdoutBuffer.append(String(it, Const.DEFAULT_TRANSPORT_ENCODING)) }
                msg.stderr?.let { stderrBuffer.append(String(it, Const.DEFAULT_TRANSPORT_ENCODING)) }
                msg.exitCode?.let { exitCode = it }
            }

            assertThat(stdoutBuffer.toString()).isEqualTo("foo\n")
            assertThat(stderrBuffer.toString()).isEqualTo("bar\n")
            assertThat(exitCode).isEqualTo(17)
        }
    }
}
