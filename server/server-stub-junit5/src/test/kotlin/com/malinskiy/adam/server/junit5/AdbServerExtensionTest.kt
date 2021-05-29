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

package com.malinskiy.adam.server.junit5

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.malinskiy.adam.AndroidDebugBridgeClient
import com.malinskiy.adam.request.misc.GetAdbServerVersionRequest
import com.malinskiy.adam.request.shell.v1.ShellCommandRequest
import com.malinskiy.adam.server.stub.AndroidDebugBridgeServer
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(AdbServerExtension::class)
class AdbServerExtensionTest {
    @AdbClient
    lateinit var client: AndroidDebugBridgeClient

    @AdbServer
    lateinit var server: AndroidDebugBridgeServer

    @Test
    fun testX() {
        server.session {
            expectAdbServerVersion()
                .accept()
                .respondAdbServerVersion(41)
        }

        runBlocking {
            val version = client.execute(GetAdbServerVersionRequest())
            assert(version == 41)
        }
    }

    @Test
    fun testMultisession() {
        runBlocking {
            server.multipleSessions {
                serial("emulator-5554") {
                    session {
                        respondOkay()
                        expectShell { "echo 1;echo x$?" }.accept().respond("1x0")
                    }
                    session {
                        respondOkay()
                        expectShell { "echo 3;echo x$?" }.accept().respond("3x0")
                    }
                }
                serial("emulator-5556") {
                    session {
                        respondOkay()
                        expectShell { "echo 2;echo x$?" }.accept().respond("2x0")
                    }
                    session {
                        respondOkay()
                        expectShell { "echo 4;echo x$?" }.accept().respond("4x0")
                    }
                }
            }

            assertThat(client.execute(ShellCommandRequest("echo 1"), "emulator-5554").output).isEqualTo("1")
            assertThat(client.execute(ShellCommandRequest("echo 2"), "emulator-5556").output).isEqualTo("2")
            assertThat(client.execute(ShellCommandRequest("echo 3"), "emulator-5554").output).isEqualTo("3")
            assertThat(client.execute(ShellCommandRequest("echo 4"), "emulator-5556").output).isEqualTo("4")
        }
    }
}
