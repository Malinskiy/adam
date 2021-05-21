/*
 * Copyright (C) 2019 Anton Malinskiy
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

package com.malinskiy.adam.request

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.malinskiy.adam.Const
import com.malinskiy.adam.request.prop.GetSinglePropRequest
import com.malinskiy.adam.server.stub.AndroidDebugBridgeServer
import kotlinx.coroutines.runBlocking
import org.junit.Test

class GetSinglePropRequestTest {
    @Test
    fun testGetX() {
        assertThat(String(GetSinglePropRequest("x").serialize(), Const.DEFAULT_TRANSPORT_ENCODING))
            .isEqualTo("0018shell:getprop x;echo x$?")
    }

    @Test
    fun testReturnsProperContent() {
        runBlocking {
            val server = AndroidDebugBridgeServer()
            val client = server.startAndListen { input, output ->
                val transportCmd = input.receiveCommand()
                assertThat(transportCmd).isEqualTo("host:transport:serial")
                output.respond(Const.Message.OKAY)

                val shellCmd = input.receiveCommand()
                assertThat(shellCmd).isEqualTo("shell:getprop prop1;echo x$?")
                output.respond(Const.Message.OKAY)

                val response = "testingx0".toByteArray(Const.DEFAULT_TRANSPORT_ENCODING)
                output.writeFully(response, 0, response.size)
            }

            val version = client.execute(GetSinglePropRequest("prop1"), serial = "serial")
            assertThat(version).isEqualTo("testing")

            server.dispose()
        }
    }
}
