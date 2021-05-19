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

package com.malinskiy.adam.request.adbd

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.malinskiy.adam.Const
import com.malinskiy.adam.extension.toRequestString
import com.malinskiy.adam.server.AndroidDebugBridgeServer
import io.ktor.utils.io.close
import kotlinx.coroutines.runBlocking
import org.junit.Test

/**
 * Restarts the on device adbd
 */
class RestartAdbdRequestTest {
    @Test
    fun testSerializeRootMode() {
        val bytes = RestartAdbdRequest(RootAdbdMode).serialize()
        assertThat(bytes.toRequestString()).isEqualTo("0005root:")
    }

    @Test
    fun testSerializeUnrootMode() {
        val bytes = RestartAdbdRequest(UnrootAdbdMode).serialize()
        assertThat(bytes.toRequestString()).isEqualTo("0007unroot:")
    }

    @Test
    fun testSerializeUsbMode() {
        val bytes = RestartAdbdRequest(UsbAdbdMode).serialize()
        assertThat(bytes.toRequestString()).isEqualTo("0004usb:")
    }

    @Test
    fun testSerializeTcpIpMode() {
        val bytes = RestartAdbdRequest(TcpIpAdbdMode(4444)).serialize()
        assertThat(bytes.toRequestString()).isEqualTo("000Atcpip:4444")
    }

    @Test
    fun testReturnsResultString() {
        runBlocking {
            val server = AndroidDebugBridgeServer()

            val client = server.startAndListen { input, output ->
                val transportCmd = input.receiveCommand()
                assertThat(transportCmd).isEqualTo("host:transport:serial")
                output.respond(Const.Message.OKAY)

                val cmd = input.receiveCommand()
                assertThat(cmd).isEqualTo("root:")
                output.respondOkay()

                output.respondStringRaw("adbd cannot run as root in production builds")
                output.close()
            }

            val output = client.execute(RestartAdbdRequest(RootAdbdMode), "serial")
            assertThat(output).isEqualTo("adbd cannot run as root in production builds")

            server.dispose()
        }
    }
}
