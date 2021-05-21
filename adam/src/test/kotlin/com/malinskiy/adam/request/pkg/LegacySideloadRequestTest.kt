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

package com.malinskiy.adam.request.pkg

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.malinskiy.adam.Const
import com.malinskiy.adam.extension.toRequestString
import com.malinskiy.adam.server.stub.AndroidDebugBridgeServer
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class LegacySideloadRequestTest {
    @Rule
    @JvmField
    val temp = TemporaryFolder()

    @Test
    fun testSerialize() {
        assertThat(LegacySideloadRequest(temp.newFile()).serialize().toRequestString()).isEqualTo("000Asideload:0")
    }

    @Test
    fun testTransfer() {
        runBlocking {
            val fixture = File(SideloadRequestTest::class.java.getResource("/fixture/sample.yaml").file)

            val server = AndroidDebugBridgeServer()

            val client = server.startAndListen { input, output ->
                val transportCmd = input.receiveCommand()
                assertThat(transportCmd).isEqualTo("host:transport:serial")
                output.respond(Const.Message.OKAY)

                val actualCommand = input.receiveCommand()
                assertThat(actualCommand).isEqualTo("sideload:614")
                output.respond(Const.Message.OKAY)

                input.receiveBytes(614)
                output.respondOkay()
            }

            val request = LegacySideloadRequest(fixture)
            val result = client.execute(request, "serial")
            assertThat(result).isTrue()

            server.dispose()
        }
    }

    @Test
    fun testTransferFailure() {
        runBlocking {
            val fixture = File(SideloadRequestTest::class.java.getResource("/fixture/sample.yaml").file)

            val server = AndroidDebugBridgeServer()

            val client = server.startAndListen { input, output ->
                val transportCmd = input.receiveCommand()
                assertThat(transportCmd).isEqualTo("host:transport:serial")
                output.respond(Const.Message.OKAY)

                val actualCommand = input.receiveCommand()
                assertThat(actualCommand).isEqualTo("sideload:614")
                output.respond(Const.Message.OKAY)

                input.receiveBytes(614)
                output.respondFail("something-something")
            }

            val request = LegacySideloadRequest(fixture)
            val result = client.execute(request, "serial")
            assertThat(result).isFalse()

            server.dispose()
        }
    }

    @Test
    fun testValidation() {
        assertThat(LegacySideloadRequest(temp.newFile()).validate().success).isTrue()
        assertThat(LegacySideloadRequest(temp.newFolder()).validate().success).isFalse()
        assertThat(LegacySideloadRequest(temp.newFile().apply { delete() }).validate().success).isFalse()
    }
}
