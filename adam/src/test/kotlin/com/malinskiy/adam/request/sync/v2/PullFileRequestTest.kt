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

package com.malinskiy.adam.request.sync.v2

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.malinskiy.adam.AndroidDebugBridgeClient
import com.malinskiy.adam.Const
import com.malinskiy.adam.exception.PullFailedException
import com.malinskiy.adam.exception.UnsupportedSyncProtocolException
import com.malinskiy.adam.request.Feature
import com.malinskiy.adam.server.junit4.AdbServerRule
import io.ktor.utils.io.discard
import io.ktor.utils.io.writeIntLittleEndian
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class PullFileRequestTest {

    @Rule
    @JvmField
    val temp = TemporaryFolder()

    @get:Rule
    val server = AdbServerRule()
    val client: AndroidDebugBridgeClient
        get() = server.client

    @Test
    fun testSerialize() {
        assertThat(
            String(
                PullFileRequest("/sdcard/testfile", File("/tmp/testfile"), listOf(Feature.SENDRECV_V2)).serialize(),
                Const.DEFAULT_TRANSPORT_ENCODING
            )
        )
            .isEqualTo("0005sync:")
    }

    @Test
    fun testSingleChunkHappyPath() {
        runBlocking {
            val fixture = File(PullFileRequestTest::class.java.getResource("/fixture/sample.yaml").file)
            val tempFile = temp.newFile()

            launch {
                server.session {
                    expectCmd { "host:transport:serial" }.accept()
                    expectCmd { "sync:" }.accept()

                    expectStat { "/sdcard/testfile" }
                    respondStat(size = fixture.length().toInt())

                    expectRecv2 { "/sdcard/testfile" }
                        .respondFile(fixture)
                        .respondDoneDone()

                    input.discard()
                }

                val request =
                    PullFileRequest("/sdcard/testfile", tempFile, listOf(Feature.SENDRECV_V2), coroutineContext = coroutineContext)
                val execute = client.execute(request, this, "serial")

                var progress = 0.0
                while (!execute.isClosedForReceive) {
                    progress = execute.receiveCatching().getOrNull() ?: break
                }

                assertThat(progress).isEqualTo(1.0)
            }.join()

            assertThat(tempFile.readBytes()).isEqualTo(fixture.readBytes())
        }
    }

    @Test
    fun testMultipleChunksHappyPath() {
        runBlocking {
            val fixture = File(PullFileRequestTest::class.java.getResource("/fixture/sample.yaml").file)
            val tempFile = temp.newFile()

            launch {
                server.session {
                    expectCmd { "host:transport:serial" }.accept()
                    expectCmd { "sync:" }.accept()

                    expectStat { "/sdcard/testfile" }
                    respondStat(size = fixture.length().toInt())

                    expectRecv2 { "/sdcard/testfile" }

                    val fileBytes = fixture.readBytes().asSequence().chunked(100)
                    val iterator = fileBytes.iterator()
                    while (iterator.hasNext()) {
                        output.respondData(iterator.next().toByteArray())
                    }
                    output.respondDoneDone()
                }

                val request =
                    PullFileRequest("/sdcard/testfile", tempFile, listOf(Feature.SENDRECV_V2), coroutineContext = coroutineContext)
                val execute = client.execute(request, this, "serial")

                var progress = 0.0
                while (!execute.isClosedForReceive) {
                    progress = execute.receiveCatching().getOrNull() ?: break
                }

                assertThat(progress).isEqualTo(1.0)
            }.join()

            assertThat(tempFile.readBytes()).isEqualTo(fixture.readBytes())
        }
    }

    @Test(expected = PullFailedException::class)
    fun testTransportFail() = runBlocking {
        val fixture = File(PullFileRequestTest::class.java.getResource("/fixture/sample.yaml").file)
        val tempFile = temp.newFile()

        launch {
            server.session {
                expectCmd { "host:transport:serial" }.accept()
                expectCmd { "sync:" }.accept()

                expectStat { "/sdcard/testfile" }
                respondStat(size = fixture.length().toInt())

                expectRecv2 { "/sdcard/testfile" }

                output.respond(Const.Message.FAIL)
                output.respondStringV2("lorem ipsum")
            }

            val request = PullFileRequest("/sdcard/testfile", tempFile, listOf(Feature.SENDRECV_V2))
            val execute = client.execute(request, this, "serial")

            var progress = 0.0
            while (!execute.isClosedForReceive) {
                progress = execute.receiveCatching().getOrNull() ?: break
            }
        }.join()
    }

    @Test(expected = UnsupportedSyncProtocolException::class)
    fun testTransportPacketSizeFailure() {
        runBlocking {
            val fixture = File(PullFileRequestTest::class.java.getResource("/fixture/sample.yaml").file)
            val tempFile = temp.newFile()

            launch {
                server.session {
                    expectCmd { "host:transport:serial" }.accept()
                    expectCmd { "sync:" }.accept()

                    expectStat { "/sdcard/testfile" }
                    respondStat(size = fixture.length().toInt())

                    expectRecv2 { "/sdcard/testfile" }

                    output.respond(Const.Message.DATA)
                    output.writeIntLittleEndian(Const.MAX_FILE_PACKET_LENGTH + 1)
                }

                val request = PullFileRequest("/sdcard/testfile", tempFile, listOf(Feature.SENDRECV_V2))
                val execute = client.execute(request, this, "serial")

                var progress = 0.0
                while (!execute.isClosedForReceive) {
                    progress = execute.receiveCatching().getOrNull() ?: break
                }
            }.join()
        }
    }

    @Test(expected = UnsupportedSyncProtocolException::class)
    fun testTransportUnsupportedPacket() {
        runBlocking {
            val fixture = File(PullFileRequestTest::class.java.getResource("/fixture/sample.yaml").file)
            val tempFile = temp.newFile()

            launch {
                server.session {
                    expectCmd { "host:transport:serial" }.accept()
                    expectCmd { "sync:" }.accept()

                    expectStat { "/sdcard/testfile" }
                    respondStat(size = fixture.length().toInt())

                    expectRecv2 { "/sdcard/testfile" }

                    output.respond(Const.Message.SEND_V1)
                    output.respond(Const.Message.SEND_V1)
                }

                val request = PullFileRequest("/sdcard/testfile", tempFile, listOf(Feature.SENDRECV_V2))
                val execute = client.execute(request, this, "serial")

                var progress = 0.0
                while (!execute.isClosedForReceive) {
                    progress = execute.receiveCatching().getOrNull() ?: break
                }
            }.join()
        }
    }
}
