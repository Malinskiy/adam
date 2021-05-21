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

package com.malinskiy.adam

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.malinskiy.adam.exception.RequestRejectedException
import com.malinskiy.adam.exception.RequestValidationException
import com.malinskiy.adam.request.ComplexRequest
import com.malinskiy.adam.request.ValidationResponse
import com.malinskiy.adam.request.shell.v1.ShellCommandRequest
import com.malinskiy.adam.server.junit4.AdbServerRule
import com.malinskiy.adam.transport.Socket
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class AndroidDebugBridgeClientTest {
    @get:Rule
    val server = AdbServerRule()
    val client: AndroidDebugBridgeClient
        get() = server.client

    @Test(expected = RequestRejectedException::class)
    fun testRequestRejection() {
        runBlocking {
            server.session {
                expectCmd { "host:transport:serial" }.reject("something-something")
            }

            val output = client.execute(ShellCommandRequest("xx"), serial = "serial")
            assertThat(output.output).isEqualTo("something-something")
            assertThat(output.exitCode).isEqualTo(0)
        }
    }

    @Test(expected = RequestRejectedException::class)
    fun testRequestRejectionDuringHandshake() {
        runBlocking {
            server.session {
                expectCmd { "host:transport:serial" }.accept()
                expectShell { "xx;echo x$?" }.reject("something-somethingx0")
            }

            client.execute(ShellCommandRequest("xx"), serial = "serial")
        }
    }

    @Test(expected = RequestRejectedException::class)
    fun testRequestRejectionDuringHandshakeWithNoMessage() {
        runBlocking {
            server.session {
                expectCmd { "host:transport:serial" }.accept()

                expectShell { "xx;echo x\$?" }
                output.respond(Const.Message.FAIL)
                output.respondStringRaw("XXXXx")
            }

            client.execute(ShellCommandRequest("xx"), serial = "serial")
        }
    }

    @Test(expected = RequestRejectedException::class)
    fun testRequestRejectionAndUnexpectedMessageLength() {
        runBlocking {
            server.session {
                expectCmd { "host:transport:serial" }

                output.respond(Const.Message.FAIL)
                output.respondStringRaw("XXXXsomething-something")
            }

            val output = client.execute(ShellCommandRequest("xx"), serial = "serial")
            assertThat(output.output).isEqualTo("something-something")
        }
    }

    @Test(expected = RequestValidationException::class)
    fun testRequestValidation() {
        runBlocking {
            client.execute(object : ComplexRequest<String>() {
                override fun validate() = ValidationResponse(false, "Fake")
                override suspend fun readElement(socket: Socket): String {
                    TODO("Not yet implemented")
                }

                override fun serialize(): ByteArray {
                    TODO("Not yet implemented")
                }
            }, serial = "serial")
        }
    }
}
