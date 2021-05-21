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
import com.malinskiy.adam.AndroidDebugBridgeClient
import com.malinskiy.adam.extension.toRequestString
import com.malinskiy.adam.server.junit4.AdbServerRule
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class ConnectDeviceRequestTest {
    @get:Rule
    val server = AdbServerRule()
    val client: AndroidDebugBridgeClient
        get() = server.client

    @Test
    fun testSerialize() {
        val bytes = ConnectDeviceRequest("123.123.123.123").serialize()
        assertThat(bytes.toRequestString()).isEqualTo("0021host:connect:123.123.123.123:5555")
    }

    @Test
    fun testSerializeWithPort() {
        val bytes = ConnectDeviceRequest("123.123.123.123", 8888).serialize()
        assertThat(bytes.toRequestString()).isEqualTo("0021host:connect:123.123.123.123:8888")
    }

    @Test
    fun testReturnsResultString() {
        runBlocking {
            server.session {
                expectCmd { "host:connect:123.123.123.123:8888" }.accept()
                respondConnectDevice("connected to 123.123.123.123:8888")
            }

            val output = client.execute(ConnectDeviceRequest("123.123.123.123", 8888))
            assertThat(output).isEqualTo("connected to 123.123.123.123:8888")
        }
    }
}
