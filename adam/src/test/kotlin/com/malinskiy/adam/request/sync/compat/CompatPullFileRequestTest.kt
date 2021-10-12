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
import com.malinskiy.adam.request.Feature
import com.malinskiy.adam.server.junit4.AdbServerRule
import io.ktor.utils.io.discard
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

    @get:Rule
    val server = AdbServerRule()
    val client: AndroidDebugBridgeClient
        get() = server.client

    @Test
    fun testV1() {
        runBlocking {
            val fixture = File(CompatPullFileRequestTest::class.java.getResource("/fixture/sample.yaml").file)
            val tempFile = temp.newFile()

            launch {
                server.session {
                    expectCmd { "host:transport:serial" }.accept()
                    expectCmd { "sync:" }.accept()

                    expectStat { "/sdcard/testfile" }
                    respondStat(size = fixture.length().toInt())

                    expectRecv { "/sdcard/testfile" }
                        .respondFile(fixture)
                        .respondDoneDone()

                    input.discard()
                }

                val request = CompatPullFileRequest("/sdcard/testfile", tempFile, emptyList(), this)
                val execute = client.execute(request, "serial")

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
    fun testV2() {
        runBlocking {
            val fixture = File(CompatPullFileRequestTest::class.java.getResource("/fixture/sample.yaml").file)
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

                val request = CompatPullFileRequest("/sdcard/testfile", tempFile, listOf(Feature.SENDRECV_V2), this)
                val execute = client.execute(request, "serial")

                var progress = 0.0
                for (update in execute) {
                    progress = update
                }
                assertThat(progress).isEqualTo(1.0)
            }.join()

            assertThat(tempFile.readBytes()).isEqualTo(fixture.readBytes())
        }
    }
}
