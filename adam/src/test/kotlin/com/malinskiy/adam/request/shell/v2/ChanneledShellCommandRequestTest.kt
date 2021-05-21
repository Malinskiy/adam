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
import com.malinskiy.adam.Const
import com.malinskiy.adam.server.stub.AndroidDebugBridgeServer
import io.ktor.utils.io.discard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Test

class ChanneledShellCommandRequestTest {
    @Test
    fun testReturnsProperContent() {
        runBlocking {
            val server = AndroidDebugBridgeServer()

            val client = server.startAndListen { input, output ->
                val transportCmd = input.receiveCommand()
                assertThat(transportCmd).isEqualTo("host:transport:serial")
                output.respond(Const.Message.OKAY)

                val shellCmd = input.receiveCommand()
                assertThat(shellCmd).isEqualTo("shell,v2,raw:echo foo; echo bar >&2; exit 17")
                output.respond(Const.Message.OKAY)

                val stdin = input.receiveShellV2Stdin()
                assertThat(stdin).isEqualTo("cafebabe")

                input.receiveShellV2StdinClose()

                output.respondShellV2Stdout("fo")
                output.respondShellV2Stderr("ba")
                output.respondShellV2Stdout("o\n")
                output.respondShellV2WindowSizeChange()
                output.respondShellV2Invalid()
                output.respondShellV2Stderr("r\n")
                output.respondShellV2Exit(17)

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
                stdio.send(ShellCommandInputChunk("cafebabe"))
                stdio.send(ShellCommandInputChunk(close = true))
            }

            for (msg in updates) {
                if (msg.stdout != null) stdoutBuffer.append(msg.stdout)
                if (msg.stderr != null) stderrBuffer.append(msg.stderr)
                if (msg.exitCode != null) exitCode = msg.exitCode!!
            }

            assertThat(stdoutBuffer.toString()).isEqualTo("foo\n")
            assertThat(stderrBuffer.toString()).isEqualTo("bar\n")
            assertThat(exitCode).isEqualTo(17)

            server.dispose()
        }
    }
}
