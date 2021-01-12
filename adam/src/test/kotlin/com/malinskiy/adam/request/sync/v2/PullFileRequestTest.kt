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
import com.malinskiy.adam.Const
import com.malinskiy.adam.exception.PullFailedException
import com.malinskiy.adam.exception.UnsupportedSyncProtocolException
import com.malinskiy.adam.request.Feature
import com.malinskiy.adam.server.AndroidDebugBridgeServer
import io.ktor.utils.io.writeIntLittleEndian
import kotlinx.coroutines.channels.receiveOrNull
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
                val server = AndroidDebugBridgeServer()

                val client = server.startAndListen { input, output ->
                    val transportCmd = input.receiveCommand()
                    assertThat(transportCmd).isEqualTo("host:transport:serial")
                    output.respond(Const.Message.OKAY)

                    val actualCommand = input.receiveCommand()
                    assertThat(actualCommand).isEqualTo("sync:")
                    output.respond(Const.Message.OKAY)

                    val statPath = input.receiveStat()
                    assertThat(statPath).isEqualTo("/sdcard/testfile")
                    output.respondStat(fixture.length().toInt())

                    val recvPath = input.receiveRecv2()
                    assertThat(recvPath).isEqualTo("/sdcard/testfile")

                    output.respondData(fixture.readBytes())
                    output.respondDone()
                    output.respondDone()
                }

                val request =
                    PullFileRequest("/sdcard/testfile", tempFile, listOf(Feature.SENDRECV_V2), coroutineContext = coroutineContext)
                val execute = client.execute(request, this, "serial")

                var progress = 0.0
                while (!execute.isClosedForReceive) {
                    progress = execute.receiveOrNull() ?: break
                }

                assertThat(progress).isEqualTo(1.0)

                server.dispose()
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
                val server = AndroidDebugBridgeServer()
                val client = server.startAndListen { input, output ->
                    val transportCmd = input.receiveCommand()
                    assertThat(transportCmd).isEqualTo("host:transport:serial")
                    output.respond(Const.Message.OKAY)

                    val actualCommand = input.receiveCommand()
                    assertThat(actualCommand).isEqualTo("sync:")
                    output.respond(Const.Message.OKAY)

                    val statPath = input.receiveStat()
                    assertThat(statPath).isEqualTo("/sdcard/testfile")
                    output.respondStat(fixture.length().toInt())

                    val recvPath = input.receiveRecv2()
                    assertThat(recvPath).isEqualTo("/sdcard/testfile")

                    val fileBytes = fixture.readBytes().asSequence().chunked(100)
                    val iterator = fileBytes.iterator()
                    while (iterator.hasNext()) {
                        output.respondData(iterator.next().toByteArray())
                    }
                    output.respondDone()
                    output.respondDone()
                }

                val request =
                    PullFileRequest("/sdcard/testfile", tempFile, listOf(Feature.SENDRECV_V2), coroutineContext = coroutineContext)
                val execute = client.execute(request, this, "serial")

                var progress = 0.0
                while (!execute.isClosedForReceive) {
                    progress = execute.receiveOrNull() ?: break
                }

                assertThat(progress).isEqualTo(1.0)

                server.dispose()
            }.join()

            assertThat(tempFile.readBytes()).isEqualTo(fixture.readBytes())
        }
    }

    @Test(expected = PullFailedException::class)
    fun testTransportFail() = runBlocking {
        val fixture = File(PullFileRequestTest::class.java.getResource("/fixture/sample.yaml").file)
        val tempFile = temp.newFile()

        launch {
            val server = AndroidDebugBridgeServer()

            val client = server.startAndListen { input, output ->
                val transportCmd = input.receiveCommand()
                assertThat(transportCmd).isEqualTo("host:transport:serial")
                output.respond(Const.Message.OKAY)

                val actualCommand = input.receiveCommand()
                assertThat(actualCommand).isEqualTo("sync:")
                output.respond(Const.Message.OKAY)

                val statPath = input.receiveStat()
                assertThat(statPath).isEqualTo("/sdcard/testfile")
                output.respondStat(fixture.length().toInt())

                val recvPath = input.receiveRecv2()
                assertThat(recvPath).isEqualTo("/sdcard/testfile")

                output.respond(Const.Message.FAIL)
                val message = "lorem ipsum"
                output.writeIntLittleEndian(message.length)
                output.respondData(message.toByteArray(Const.DEFAULT_TRANSPORT_ENCODING))
            }

            val request = PullFileRequest("/sdcard/testfile", tempFile, listOf(Feature.SENDRECV_V2))
            val execute = client.execute(request, this, "serial")

            var progress = 0.0
            while (!execute.isClosedForReceive) {
                progress = execute.receiveOrNull() ?: break
            }

            server.dispose()
        }.join()
    }

    @Test(expected = UnsupportedSyncProtocolException::class)
    fun testTransportPacketSizeFailure() {
        runBlocking {
            val fixture = File(PullFileRequestTest::class.java.getResource("/fixture/sample.yaml").file)
            val tempFile = temp.newFile()

            launch {
                val server = AndroidDebugBridgeServer()

                val client = server.startAndListen { input, output ->
                    val transportCmd = input.receiveCommand()
                    assertThat(transportCmd).isEqualTo("host:transport:serial")
                    output.respond(Const.Message.OKAY)

                    val actualCommand = input.receiveCommand()
                    assertThat(actualCommand).isEqualTo("sync:")
                    output.respond(Const.Message.OKAY)

                    val statPath = input.receiveStat()
                    assertThat(statPath).isEqualTo("/sdcard/testfile")
                    output.respondStat(fixture.length().toInt())

                    val recvPath = input.receiveRecv2()
                    assertThat(recvPath).isEqualTo("/sdcard/testfile")

                    output.respond(Const.Message.DATA)
                    output.respondData(ByteArray(Const.MAX_FILE_PACKET_LENGTH + 1))
                }

                val request = PullFileRequest("/sdcard/testfile", tempFile, listOf(Feature.SENDRECV_V2))
                val execute = client.execute(request, this, "serial")

                var progress = 0.0
                while (!execute.isClosedForReceive) {
                    progress = execute.receiveOrNull() ?: break
                }

                server.dispose()
            }.join()
        }
    }

    @Test(expected = UnsupportedSyncProtocolException::class)
    fun testTransportUnsupportedPacket() {
        runBlocking {
            val fixture = File(PullFileRequestTest::class.java.getResource("/fixture/sample.yaml").file)
            val tempFile = temp.newFile()

            launch {
                val server = AndroidDebugBridgeServer()

                val client = server.startAndListen { input, output ->
                    val transportCmd = input.receiveCommand()
                    assertThat(transportCmd).isEqualTo("host:transport:serial")
                    output.respond(Const.Message.OKAY)

                    val actualCommand = input.receiveCommand()
                    assertThat(actualCommand).isEqualTo("sync:")
                    output.respond(Const.Message.OKAY)

                    val statPath = input.receiveStat()
                    assertThat(statPath).isEqualTo("/sdcard/testfile")
                    output.respondStat(fixture.length().toInt())

                    val recvPath = input.receiveRecv2()
                    assertThat(recvPath).isEqualTo("/sdcard/testfile")

                    output.respond(Const.Message.SEND_V1)
                    output.respond(Const.Message.SEND_V1)
                }

                val request = PullFileRequest("/sdcard/testfile", tempFile, listOf(Feature.SENDRECV_V2))
                val execute = client.execute(request, this, "serial")

                var progress = 0.0
                while (!execute.isClosedForReceive) {
                    progress = execute.receiveOrNull() ?: break
                }

                server.dispose()
            }.join()
        }
    }
}
