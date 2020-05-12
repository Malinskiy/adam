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
import assertk.assertions.isEqualTo
import com.malinskiy.adam.Const
import com.malinskiy.adam.exception.PushFailedException
import com.malinskiy.adam.server.AndroidDebugBridgeServer
import io.ktor.utils.io.close
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.receiveOrNull
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.io.File
import kotlin.coroutines.CoroutineContext

class PushFileRequestTest : CoroutineScope {
    @Test
    fun testSerialize() {
        val testFile = File.createTempFile("adam", "y")
        val fileName = testFile.name
        val bytes = PushFileRequest(testFile, "/data/local/tmp/$fileName").serialize()
        assertThat(bytes.toString(Const.DEFAULT_TRANSPORT_ENCODING))
            .isEqualTo("0005sync:")
    }

    @Test
    fun testSingleChunkHappyPath() {
        val finishedProgress = runBlocking {
            val fixture = File(PushFileRequestTest::class.java.getResource("/fixture/sample.yaml").file)

            val server = AndroidDebugBridgeServer()

            var receiveFile: File? = null

            val client = server.startAndListen { input, output ->
                val transportCmd = input.receiveCommand()
                assertThat(transportCmd).isEqualTo("host:transport:serial")
                output.respond(Const.Message.OKAY)

                val actualCommand = input.receiveCommand()
                assertThat(actualCommand).isEqualTo("sync:")
                output.respond(Const.Message.OKAY)

                val receiveCmd = input.receiveSend()
                assertThat(receiveCmd).isEqualTo("/sdcard/testfile,420")
                receiveFile = input.receiveFile()
                output.respond(Const.Message.OKAY)
                output.close()
            }

            val request = PushFileRequest(fixture, "/sdcard/testfile")
            val execute = client.execute(request, this@PushFileRequestTest, "serial")

            var progress = 0.0
            while (!execute.isClosedForReceive) {
                progress = execute.receiveOrNull() ?: break
            }

            assertThat(receiveFile!!.readBytes()).isEqualTo(fixture.readBytes())

            server.dispose()

            return@runBlocking progress
        }

        assertThat(finishedProgress).isEqualTo(1.0)
    }

    @Test(expected = PushFailedException::class)
    fun testTransportFailureOnDone() {
        val finishedProgress = runBlocking {
            val fixture = File(PushFileRequestTest::class.java.getResource("/fixture/sample.yaml").file)

            val server = AndroidDebugBridgeServer()

            var receiveFile: File? = null

            val client = server.startAndListen { input, output ->
                val transportCmd = input.receiveCommand()
                assertThat(transportCmd).isEqualTo("host:transport:serial")
                output.respond(Const.Message.OKAY)

                val actualCommand = input.receiveCommand()
                assertThat(actualCommand).isEqualTo("sync:")
                output.respond(Const.Message.OKAY)

                val receiveCmd = input.receiveSend()
                assertThat(receiveCmd).isEqualTo("/sdcard/testfile,420")
                receiveFile = input.receiveFile()
                output.respond(Const.Message.FAIL)
                val s = "CAFEBABE"
                output.writeFully("0008".toByteArray(Const.DEFAULT_TRANSPORT_ENCODING), 0, 4)
                output.writeFully(s.toByteArray(Const.DEFAULT_TRANSPORT_ENCODING), 0, s.length)
                output.close()
            }

            val request = PushFileRequest(fixture, "/sdcard/testfile")
            val execute = client.execute(request, this@PushFileRequestTest, "serial")

            var progress = 0.0
            while (!execute.isClosedForReceive) {
                progress = execute.receiveOrNull() ?: break
            }

            assertThat(receiveFile!!.readBytes()).isEqualTo(fixture.readBytes())

            server.dispose()

            return@runBlocking progress
        }

        assertThat(finishedProgress).isEqualTo(1.0)
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO
}