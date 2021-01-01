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

package com.malinskiy.adam.request.sync

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import com.malinskiy.adam.Const
import com.malinskiy.adam.request.prop.GetPropRequest
import com.malinskiy.adam.server.AndroidDebugBridgeServer
import io.ktor.utils.io.*
import kotlinx.coroutines.runBlocking
import org.junit.Test

class GetPropRequestTest {
    @Test
    fun testGetAll() {
        assertThat(String(GetPropRequest().serialize(), Const.DEFAULT_TRANSPORT_ENCODING))
            .isEqualTo("0016shell:getprop;echo x$?")
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
                assertThat(shellCmd).isEqualTo("shell:getprop;echo x$?")
                output.respond(Const.Message.OKAY)

                val response = "[testing]: [testing]\r\r\nx0".toByteArray(Const.DEFAULT_TRANSPORT_ENCODING)
                output.writeFully(response, 0, response.size)
                output.close()
            }

            val version = client.execute(GetPropRequest(), serial = "serial")
            assertThat(version).contains("testing", "testing")

            server.dispose()
        }
    }
}
