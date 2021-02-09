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
import com.malinskiy.adam.server.AndroidDebugBridgeServer
import com.malinskiy.adam.transport.Socket
import io.ktor.utils.io.*
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.net.Socket

class AndroidDebugBridgeClientTest {
    @Test(expected = RequestRejectedException::class)
    fun testRequestRejection() {
        runBlocking {
            val server = AndroidDebugBridgeServer()

            val client = server.startAndListen { input, output ->
                val transportCmd = input.receiveCommand()
                assertThat(transportCmd).isEqualTo("host:transport:serial")
                output.respond(Const.Message.FAIL)

                val response = "0013something-somethingx0".toByteArray(Const.DEFAULT_TRANSPORT_ENCODING)
                output.writeFully(response, 0, response.size)
                output.close()
            }

            val output = client.execute(ShellCommandRequest("xx"), serial = "serial")
            assertThat(output.output).isEqualTo("something-something")
            assertThat(output.exitCode).isEqualTo(0)

            server.dispose()
        }
    }

    @Test(expected = RequestRejectedException::class)
    fun testRequestRejectionDuringHandshake() {
        runBlocking {
            val server = AndroidDebugBridgeServer()

            val client = server.startAndListen { input, output ->
                val transportCmd = input.receiveCommand()
                assertThat(transportCmd).isEqualTo("host:transport:serial")
                output.respond(Const.Message.OKAY)

                val shellCmd = input.receiveCommand()
                assertThat(shellCmd).isEqualTo("shell:xx;echo x$?")
                output.respond(Const.Message.FAIL)

                val response = "0013something-somethingx0".toByteArray(Const.DEFAULT_TRANSPORT_ENCODING)
                output.writeFully(response, 0, response.size)
                output.close()
            }

            client.execute(ShellCommandRequest("xx"), serial = "serial")
            server.dispose()
        }
    }

    @Test(expected = RequestRejectedException::class)
    fun testRequestRejectionDuringHandshakeWithNoMessage() {
        runBlocking {
            val server = AndroidDebugBridgeServer()

            val client = server.startAndListen { input, output ->
                val transportCmd = input.receiveCommand()
                assertThat(transportCmd).isEqualTo("host:transport:serial")
                output.respond(Const.Message.OKAY)

                val shellCmd = input.receiveCommand()
                assertThat(shellCmd).isEqualTo("shell:xx;echo x\$?")
                output.respond(Const.Message.FAIL)

                val response = "XXXXx".toByteArray(Const.DEFAULT_TRANSPORT_ENCODING)
                output.writeFully(response, 0, response.size)
                output.close()
            }

            client.execute(ShellCommandRequest("xx"), serial = "serial")
            server.dispose()
        }
    }

    @Test(expected = RequestRejectedException::class)
    fun testRequestRejectionAndUnexpectedMessageLength() {
        runBlocking {
            val server = AndroidDebugBridgeServer()

            val client = server.startAndListen { input, output ->
                val transportCmd = input.receiveCommand()
                assertThat(transportCmd).isEqualTo("host:transport:serial")
                output.respond(Const.Message.FAIL)

                val response = "XXXXsomething-something".toByteArray(Const.DEFAULT_TRANSPORT_ENCODING)
                output.writeFully(response, 0, response.size)
                output.close()
            }

            val output = client.execute(ShellCommandRequest("xx"), serial = "serial")
            assertThat(output.output).isEqualTo("something-something")

            server.dispose()
        }
    }

    @Test(expected = RequestValidationException::class)
    fun testRequestValidation() {
        runBlocking {
            val server = AndroidDebugBridgeServer()

            val client = server.startAndListen { _, _ ->
            }

            client.execute(object : ComplexRequest<String>() {
                override fun validate() = ValidationResponse(false, "Fake")
                override suspend fun readElement(socket: Socket): String {
                    TODO("Not yet implemented")
                }

                override fun serialize(): ByteArray {
                    TODO("Not yet implemented")
                }
            }, serial = "serial")
            server.dispose()
        }
    }
}
