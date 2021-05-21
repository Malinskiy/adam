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

package com.malinskiy.adam.request.misc

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.malinskiy.adam.extension.toRequestString
import com.malinskiy.adam.server.stub.AndroidDebugBridgeServer
import kotlinx.coroutines.runBlocking
import org.junit.Test

class DisconnectDeviceRequestTest {
    @Test
    fun testSerialize() {
        val bytes = DisconnectDeviceRequest("123.123.123.123").serialize()
        assertThat(bytes.toRequestString()).isEqualTo("0024host:disconnect:123.123.123.123:5555")
    }

    @Test
    fun testSerializeWithPort() {
        val bytes = DisconnectDeviceRequest("123.123.123.123", 8888).serialize()
        assertThat(bytes.toRequestString()).isEqualTo("0024host:disconnect:123.123.123.123:8888")
    }

    @Test
    fun testSerializeDisconnectAll() {
        val bytes = DisconnectDeviceRequest().serialize()
        assertThat(bytes.toRequestString()).isEqualTo("0010host:disconnect:")
    }

    @Test
    fun testReturnsResultString() {
        runBlocking {
            val server = AndroidDebugBridgeServer()

            val client = server.startAndListen { input, output ->
                val cmd = input.receiveCommand()
                assertThat(cmd).isEqualTo("host:disconnect:123.123.123.123:8888")
                output.respondOkay()

                output.respondStringV1("disconnected 123.123.123.123")
            }

            val output = client.execute(DisconnectDeviceRequest("123.123.123.123", 8888))
            assertThat(output).isEqualTo("disconnected 123.123.123.123")

            server.dispose()
        }
    }
}

