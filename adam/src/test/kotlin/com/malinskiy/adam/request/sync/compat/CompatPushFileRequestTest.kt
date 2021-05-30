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
import com.malinskiy.adam.AndroidDebugBridgeClient
import com.malinskiy.adam.extension.testResource
import com.malinskiy.adam.request.Feature
import com.malinskiy.adam.server.junit4.AdbServerRule
import io.ktor.utils.io.discard
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CompatPushFileRequestTest {
    @Rule
    @JvmField
    val temp = TemporaryFolder()

    @get:Rule
    val server = AdbServerRule()
    val client: AndroidDebugBridgeClient
        get() = server.client

    @Test
    fun testV1() {
        runBlocking {
            val fixture = testResource("/fixture/sample.yaml")
            val receiveFile = temp.newFile()

            launch {
                server.session {
                    expectCmd { "host:transport:serial" }.accept()

                    expectCmd { "sync:" }.accept()

                    expectSend { "/sdcard/testfile,511" }
                        .receiveFile(receiveFile)
                        .done()

                    input.discard()
                }

                val request = CompatPushFileRequest(fixture, "/sdcard/testfile", emptyList(), this)
                val execute = client.execute(request, "serial")

                var progress = 0.0
                while (!execute.isClosedForReceive) {
                    progress = execute.receiveOrNull() ?: break
                }

                assertThat(progress).isEqualTo(1.0)
            }.join()

            assertThat(receiveFile.readBytes()).isEqualTo(fixture.readBytes())
        }
    }

    @Test
    fun testV2() {
        runBlocking {
            val fixture = testResource("/fixture/sample.yaml")
            var receiveFile = temp.newFile()

            launch {
                server.session {
                    expectCmd { "host:transport:serial" }.accept()
                    expectCmd { "sync:" }.accept()

                    expectSendV2("/sdcard/testfile", "777", 0)
                        .receiveFile(receiveFile)
                        .done()

                    input.discard()
                }

                val request = CompatPushFileRequest(fixture, "/sdcard/testfile", listOf(Feature.SENDRECV_V2), this)
                val execute = client.execute(request, "serial")

                var progress = 0.0
                while (!execute.isClosedForReceive) {
                    progress = execute.receiveOrNull() ?: break
                }

                assertThat(progress).isEqualTo(1.0)
            }.join()

            assertThat(receiveFile.readBytes()).isEqualTo(fixture.readBytes())
        }
    }
}
