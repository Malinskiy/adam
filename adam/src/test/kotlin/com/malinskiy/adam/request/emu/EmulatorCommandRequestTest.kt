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

package com.malinskiy.adam.request.emu

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.malinskiy.adam.server.stub.EmulatorConsoleServer
import kotlinx.coroutines.runBlocking
import org.junit.Test

class EmulatorCommandRequestTest {
    @Test
    fun testHappyPath() {
        runBlocking {
            EmulatorConsoleServer().use { server ->
                val (client, consoleAddr) = server.startAndListen { input, output ->
                    val authToken = input.receiveAuth()
                    assertThat(authToken).isEqualTo("token")
                    output.writeAuth()

                    val cmd = input.receiveCommand()
                    assertThat(cmd).isEqualTo("help")
                    output.respond("Android console commands:")

                    input.receiveExit()
                }

                val output = client.execute(EmulatorCommandRequest("help", consoleAddr, authToken = "token"))
                assertThat(output).isEqualTo("Android console commands:")
            }
        }
    }
}
