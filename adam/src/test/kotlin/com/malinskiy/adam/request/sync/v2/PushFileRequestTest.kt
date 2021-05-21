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
import com.malinskiy.adam.exception.PushFailedException
import com.malinskiy.adam.request.Feature
import com.malinskiy.adam.server.stub.AndroidDebugBridgeServer
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class PushFileRequestTest {

    @Rule
    @JvmField
    val temp = TemporaryFolder()

    @Test
    fun testSerialize() {
        val testFile = temp.newFile("adam")
        val fileName = testFile.name
        val bytes = PushFileRequest(testFile, "/data/local/tmp/$fileName", listOf(Feature.SENDRECV_V2)).serialize()
        assertThat(bytes.toString(Const.DEFAULT_TRANSPORT_ENCODING))
            .isEqualTo("0005sync:")
    }

    @Test
    fun testSingleChunkHappyPath() {
        runBlocking {
            val fixture = File(PushFileRequestTest::class.java.getResource("/fixture/sample.yaml").file)
            val receiveFile = temp.newFile()

            launch {
                val server = AndroidDebugBridgeServer()

                val client = server.startAndListen { input, output ->
                    val transportCmd = input.receiveCommand()
                    assertThat(transportCmd).isEqualTo("host:transport:serial")
                    output.respond(Const.Message.OKAY)

                    val actualCommand = input.receiveCommand()
                    assertThat(actualCommand).isEqualTo("sync:")
                    output.respond(Const.Message.OKAY)

                    val (receiveCmd, mode, flags) = input.receiveSendV2()
                    assertThat(receiveCmd).isEqualTo("/sdcard/testfile")
                    assertThat(mode.toString(8)).isEqualTo("777")
                    assertThat(flags).isEqualTo(0)

                    input.receiveFile(receiveFile)
                    output.respond(Const.Message.OKAY)
                }

                val request = PushFileRequest(fixture, "/sdcard/testfile", listOf(Feature.SENDRECV_V2))
                val execute = client.execute(request, this, "serial")

                var progress = 0.0
                while (!execute.isClosedForReceive) {
                    progress = execute.receiveOrNull() ?: break
                }

                assertThat(progress).isEqualTo(1.0)
                server.dispose()

            }.join()

            assertThat(receiveFile.readBytes()).isEqualTo(fixture.readBytes())
        }
    }

    @Test(expected = PushFailedException::class)
    fun testTransportFailureOnDone() {
        runBlocking {
            val fixture = File(PushFileRequestTest::class.java.getResource("/fixture/sample.yaml").file)
            val receiveFile = temp.newFile()

            launch {
                val server = AndroidDebugBridgeServer()

                val client = server.startAndListen { input, output ->
                    val transportCmd = input.receiveCommand()
                    assertThat(transportCmd).isEqualTo("host:transport:serial")
                    output.respond(Const.Message.OKAY)

                    val actualCommand = input.receiveCommand()
                    assertThat(actualCommand).isEqualTo("sync:")
                    output.respond(Const.Message.OKAY)

                    val (receiveCmd, mode, flags) = input.receiveSendV2()
                    assertThat(receiveCmd).isEqualTo("/sdcard/testfile")
                    assertThat(mode.toString(8)).isEqualTo("777")
                    assertThat(flags).isEqualTo(0)

                    input.receiveFile(receiveFile)
                    output.respond(Const.Message.FAIL)
                    val s = "CAFEBABE"
                    output.writeFully("0008".toByteArray(Const.DEFAULT_TRANSPORT_ENCODING), 0, 4)
                    output.writeFully(s.toByteArray(Const.DEFAULT_TRANSPORT_ENCODING), 0, s.length)
                }

                val request = PushFileRequest(fixture, "/sdcard/testfile", listOf(Feature.SENDRECV_V2))
                val execute = client.execute(request, this, "serial")

                var progress = 0.0
                while (!execute.isClosedForReceive) {
                    progress = execute.receiveOrNull() ?: break
                }

                assertThat(receiveFile!!.readBytes()).isEqualTo(fixture.readBytes())

                server.dispose()
            }.join()
        }
    }
}
