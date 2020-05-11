/*
 * Copyright (C) 2020 Anton Malinskiy
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
import assertk.assertions.isEqualTo
import com.malinskiy.adam.Const
import com.malinskiy.adam.exception.PullFailedException
import com.malinskiy.adam.exception.UnsupportedSyncProtocolException
import com.malinskiy.adam.server.AndroidDebugBridgeServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.receiveOrNull
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.io.File
import kotlin.coroutines.CoroutineContext

class PullFileRequestTest : CoroutineScope {

    @Test
    fun testSerialize() {
        assertThat(String(PullFileRequest("/sdcard/testfile", File("/tmp/testfile")).serialize(), Const.DEFAULT_TRANSPORT_ENCODING))
            .isEqualTo("0005sync:")
    }

    @Test
    fun testSingleChunkHappyPath() {
        val finishedProgress = runBlocking {
            val fixture = File(PullFileRequestTest::class.java.getResource("/fixture/sample.yaml").file)

            val server = AndroidDebugBridgeServer()
            val client = server.buildClient()

            server.startAndListen { input, output ->
                val transportCmd = input.receiveCommand()
                assertThat(transportCmd).isEqualTo("host:transport:serial")
                output.respond(Const.Message.OKAY)

                val actualCommand = input.receiveCommand()
                assertThat(actualCommand).isEqualTo("sync:")
                output.respond(Const.Message.OKAY)

                val statPath = input.receiveStat()
                assertThat(statPath).isEqualTo("/sdcard/testfile")
                output.respondStat(fixture.length().toInt())

                val recvPath = input.receiveRecv()
                assertThat(recvPath).isEqualTo("/sdcard/testfile")

                output.respondData(fixture.readBytes())
                output.respondDone()
                output.respondDone()
            }

            val tempFile = createTempFile()
            val request = PullFileRequest("/sdcard/testfile", tempFile)
            val execute = client.execute(request, this@PullFileRequestTest, "serial")

            var progress = 0.0
            while (!execute.isClosedForReceive) {
                progress = execute.receiveOrNull() ?: break
            }

            server.dispose()

            assertThat(tempFile.readBytes()).isEqualTo(fixture.readBytes())

            return@runBlocking progress
        }

        assertThat(finishedProgress).isEqualTo(1.0)
    }

    @Test
    fun testMultipleChunksHappyPath() {
        val finishedProgress = runBlocking {
            val fixture = File(PullFileRequestTest::class.java.getResource("/fixture/sample.yaml").file)

            val server = AndroidDebugBridgeServer()
            val client = server.buildClient()

            server.startAndListen { input, output ->
                val transportCmd = input.receiveCommand()
                assertThat(transportCmd).isEqualTo("host:transport:serial")
                output.respond(Const.Message.OKAY)

                val actualCommand = input.receiveCommand()
                assertThat(actualCommand).isEqualTo("sync:")
                output.respond(Const.Message.OKAY)

                val statPath = input.receiveStat()
                assertThat(statPath).isEqualTo("/sdcard/testfile")
                output.respondStat(fixture.length().toInt())

                val recvPath = input.receiveRecv()
                assertThat(recvPath).isEqualTo("/sdcard/testfile")

                val fileBytes = fixture.readBytes().asSequence().chunked(100)
                val iterator = fileBytes.iterator()
                while (iterator.hasNext()) {
                    output.respondData(iterator.next().toByteArray())
                }
                output.respondDone()
                output.respondDone()
            }

            val tempFile = createTempFile()
            val request = PullFileRequest("/sdcard/testfile", tempFile)
            val execute = client.execute(request, this@PullFileRequestTest, "serial")

            var progress = 0.0
            while (!execute.isClosedForReceive) {
                progress = execute.receiveOrNull() ?: break
            }

            server.dispose()

            assertThat(tempFile.readBytes()).isEqualTo(fixture.readBytes())

            return@runBlocking progress
        }

        assertThat(finishedProgress).isEqualTo(1.0)
    }

    @Test(expected = PullFailedException::class)
    fun testTransportFail() {
        val finishedProgress = runBlocking {
            val fixture = File(PullFileRequestTest::class.java.getResource("/fixture/sample.yaml").file)

            val server = AndroidDebugBridgeServer()
            val client = server.buildClient()

            server.startAndListen { input, output ->
                val transportCmd = input.receiveCommand()
                assertThat(transportCmd).isEqualTo("host:transport:serial")
                output.respond(Const.Message.OKAY)

                val actualCommand = input.receiveCommand()
                assertThat(actualCommand).isEqualTo("sync:")
                output.respond(Const.Message.OKAY)

                val statPath = input.receiveStat()
                assertThat(statPath).isEqualTo("/sdcard/testfile")
                output.respondStat(fixture.length().toInt())

                val recvPath = input.receiveRecv()
                assertThat(recvPath).isEqualTo("/sdcard/testfile")

                output.respond(Const.Message.FAIL)
                output.respondDone()
            }

            val tempFile = createTempFile()
            val request = PullFileRequest("/sdcard/testfile", tempFile)
            val execute = client.execute(request, this@PullFileRequestTest, "serial")

            var progress = 0.0
            while (!execute.isClosedForReceive) {
                progress = execute.receiveOrNull() ?: break
            }

            server.dispose()

            return@runBlocking progress
        }

        assertThat(finishedProgress).isEqualTo(1.0)
    }

    @Test(expected = UnsupportedSyncProtocolException::class)
    fun testTransportPacketSizeFailure() {
        val finishedProgress = runBlocking {
            val fixture = File(PullFileRequestTest::class.java.getResource("/fixture/sample.yaml").file)

            val server = AndroidDebugBridgeServer()
            val client = server.buildClient()

            server.startAndListen { input, output ->
                val transportCmd = input.receiveCommand()
                assertThat(transportCmd).isEqualTo("host:transport:serial")
                output.respond(Const.Message.OKAY)

                val actualCommand = input.receiveCommand()
                assertThat(actualCommand).isEqualTo("sync:")
                output.respond(Const.Message.OKAY)

                val statPath = input.receiveStat()
                assertThat(statPath).isEqualTo("/sdcard/testfile")
                output.respondStat(fixture.length().toInt())

                val recvPath = input.receiveRecv()
                assertThat(recvPath).isEqualTo("/sdcard/testfile")

                output.respond(Const.Message.DATA)
                output.respondData(ByteArray(Const.MAX_FILE_PACKET_LENGTH + 1))
            }

            val tempFile = createTempFile()
            val request = PullFileRequest("/sdcard/testfile", tempFile)
            val execute = client.execute(request, this@PullFileRequestTest, "serial")

            var progress = 0.0
            while (!execute.isClosedForReceive) {
                progress = execute.receiveOrNull() ?: break
            }

            server.dispose()

            return@runBlocking progress
        }

        assertThat(finishedProgress).isEqualTo(1.0)
    }

    @Test(expected = UnsupportedSyncProtocolException::class)
    fun testTransportUnsupportedPacket() {
        val finishedProgress = runBlocking {
            val fixture = File(PullFileRequestTest::class.java.getResource("/fixture/sample.yaml").file)

            val server = AndroidDebugBridgeServer()
            val client = server.buildClient()

            server.startAndListen { input, output ->
                val transportCmd = input.receiveCommand()
                assertThat(transportCmd).isEqualTo("host:transport:serial")
                output.respond(Const.Message.OKAY)

                val actualCommand = input.receiveCommand()
                assertThat(actualCommand).isEqualTo("sync:")
                output.respond(Const.Message.OKAY)

                val statPath = input.receiveStat()
                assertThat(statPath).isEqualTo("/sdcard/testfile")
                output.respondStat(fixture.length().toInt())

                val recvPath = input.receiveRecv()
                assertThat(recvPath).isEqualTo("/sdcard/testfile")

                output.respond(Const.Message.SEND)
                output.respond(Const.Message.SEND)
            }

            val tempFile = createTempFile()
            val request = PullFileRequest("/sdcard/testfile", tempFile)
            val execute = client.execute(request, this@PullFileRequestTest, "serial")

            var progress = 0.0
            while (!execute.isClosedForReceive) {
                progress = execute.receiveOrNull() ?: break
            }

            server.dispose()

            return@runBlocking progress
        }

        assertThat(finishedProgress).isEqualTo(1.0)
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO
}