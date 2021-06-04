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
import com.malinskiy.adam.AndroidDebugBridgeClient
import com.malinskiy.adam.Const
import com.malinskiy.adam.extension.toRequestString
import com.malinskiy.adam.server.junit4.AdbServerRule
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.ByteBuffer

class SideloadRequestTest {
    @get:Rule
    val server = AdbServerRule()
    val client: AndroidDebugBridgeClient
        get() = server.client

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
            val fixture = File.createTempFile("transfer-test", "test-transfer").apply { deleteOnExit() }
            fixture.writeBytes(ByteArray(65536) { 0.toByte() })
            fixture.appendBytes(ByteArray(65536) { 1.toByte() })
            fixture.appendBytes(ByteArray(14) { 2.toByte() })

            server.session {
                expectCmd { "host:transport:serial" }.accept()
                expectCmd { "sideload-host:131086:65536" }.accept()

                respondSideloadChunkRequested("00000000")
                val chunk1 = input.receiveBytes(Const.MAX_FILE_PACKET_LENGTH)
                respondSideloadChunkRequested("00000001")
                val chunk2 = input.receiveBytes(Const.MAX_FILE_PACKET_LENGTH)
                respondSideloadChunkRequested("00000000")
                val chunk1Replay = input.receiveBytes(Const.MAX_FILE_PACKET_LENGTH)
                respondSideloadChunkRequested("00000002")
                val chunk3 = input.receiveBytes(14)

                assertThat(chunk1).isEqualTo(chunk1Replay)

                val buffer = ByteBuffer.allocate(Const.MAX_FILE_PACKET_LENGTH * 2 + 14)
                buffer.put(chunk1)
                buffer.put(chunk2)
                buffer.put(chunk3)

                val expected = fixture.readBytes()
                val actual = buffer.array()

                assertThat(actual).isEqualTo(expected)

                output.respondDoneDone()
            }

            val request = SideloadRequest(fixture)
            val result = client.execute(request, "serial")
            assertThat(result).isTrue()
        }
    }

    @Test
    fun testTransferFailure() {
        runBlocking {
            val fixture = File(SideloadRequestTest::class.java.getResource("/fixture/sample.yaml").file)

            server.session {
                expectCmd { "host:transport:serial" }.accept()
                expectCmd { "sideload-host:614:65536" }.accept()

                output.respondFailFail()
            }

            val request = SideloadRequest(fixture)
            val result = client.execute(request, "serial")
            assertThat(result).isFalse()
        }
    }

    @Test
    fun testValidation() {
        assertThat(SideloadRequest(temp.newFile()).validate().success).isTrue()
        assertThat(SideloadRequest(temp.newFolder()).validate().success).isFalse()
        assertThat(SideloadRequest(temp.newFile().apply { delete() }).validate().success).isFalse()
    }
}
