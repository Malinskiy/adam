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
import com.malinskiy.adam.server.AndroidDebugBridgeServer
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.ByteBuffer

class SideloadRequestTest {

    @Rule
    @JvmField
    val temp = TemporaryFolder()

    @Test
    fun testSerialize() {
        assertThat(SideloadRequest(temp.newFile()).serialize().toRequestString()).isEqualTo("0015sideload-host:0:65536")
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
                assertThat(actualCommand).isEqualTo("sideload-host:614:300")
                output.respond(Const.Message.OKAY)

                output.respondStringRaw("00000000")
                val chunk1 = input.receiveBytes(300)
                output.respondStringRaw("00000001")
                val chunk2 = input.receiveBytes(300)
                output.respondStringRaw("00000000")
                val chunk1Replay = input.receiveBytes(300)
                output.respondStringRaw("00000002")
                val chunk3 = input.receiveBytes(14)

                assertThat(chunk1).isEqualTo(chunk1Replay)

                val buffer = ByteBuffer.allocate(614)
                buffer.put(chunk1)
                buffer.put(chunk2)
                buffer.put(chunk3)

                val expected = fixture.readBytes()
                val actual = buffer.array()

                assertThat(actual).isEqualTo(expected)

                output.respondDoneDone()
            }

            val request = SideloadRequest(fixture, blockSize = 300)
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
                assertThat(actualCommand).isEqualTo("sideload-host:614:300")
                output.respond(Const.Message.OKAY)

                output.respondFailFail()
            }

            val request = SideloadRequest(fixture, blockSize = 300)
            val result = client.execute(request, "serial")
            assertThat(result).isFalse()

            server.dispose()
        }
    }
}
