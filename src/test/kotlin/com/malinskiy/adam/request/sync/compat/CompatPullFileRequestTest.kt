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

package com.malinskiy.adam.request.sync.compat

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.malinskiy.adam.Const
import com.malinskiy.adam.request.Feature
import com.malinskiy.adam.server.AndroidDebugBridgeServer
import io.ktor.utils.io.close
import io.ktor.utils.io.discard
import kotlinx.coroutines.channels.receiveOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class CompatPullFileRequestTest {
    @Rule
    @JvmField
    val temp = TemporaryFolder()

    @Test
    fun testV1() {
        runBlocking {
            val fixture = File(CompatPullFileRequestTest::class.java.getResource("/fixture/sample.yaml").file)
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

                    val recvPath = input.receiveRecv()
                    assertThat(recvPath).isEqualTo("/sdcard/testfile")

                    output.respondData(fixture.readBytes())
                    output.respondDone()
                    output.respondDone()

                    output.close()
                    input.discard()
                }

                val request = CompatPullFileRequest("/sdcard/testfile", tempFile, emptyList(), this)
                val execute = client.execute(request, "serial")

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
    fun testV2() {
        repeat(100) {
            runBlocking {
                val fixture = File(CompatPullFileRequestTest::class.java.getResource("/fixture/sample.yaml").file)
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

                        output.close()
                        input.discard()
                    }

                    val request = CompatPullFileRequest("/sdcard/testfile", tempFile, listOf(Feature.SENDRECV_V2), this)
                    val execute = client.execute(request, "serial")

                    var progress = 0.0
                    for (update in execute) {
                        progress = update
                    }
                    assertThat(progress).isEqualTo(1.0)

                    server.dispose()
                }.join()

                assertThat(tempFile.readBytes()).isEqualTo(fixture.readBytes())
            }
        }
    }
}
