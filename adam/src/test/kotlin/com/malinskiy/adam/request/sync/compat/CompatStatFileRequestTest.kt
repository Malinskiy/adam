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
import com.malinskiy.adam.request.sync.model.FileEntryV1
import com.malinskiy.adam.request.sync.model.FileEntryV2
import com.malinskiy.adam.server.stub.AndroidDebugBridgeServer
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.time.Instant

class CompatStatFileRequestTest {
    @Test
    fun testV1() {
        runBlocking {
            val server = AndroidDebugBridgeServer()

            val client = server.startAndListen { input, output ->
                val transportCmd = input.receiveCommand()
                assertThat(transportCmd).isEqualTo("host:transport:serial")
                output.respond(Const.Message.OKAY)

                val shellCmd = input.receiveCommand()
                assertThat(shellCmd).isEqualTo("sync:")
                output.respond(Const.Message.OKAY)

                val receiveStat = input.receiveStat()
                assertThat(receiveStat).isEqualTo("/sdcard/testfile")

                output.respondStat(128, 0x744, 10000)
            }

            val result = client.execute(CompatStatFileRequest("/sdcard/testfile", emptyList()), serial = "serial")
            val output = result as FileEntryV1
            assertThat(output.mtime).isEqualTo(Instant.ofEpochSecond(10000))
            assertThat(output.mode).isEqualTo(0x744.toUInt())
            assertThat(output.size).isEqualTo(128.toUInt())

            server.dispose()
        }
    }

    @Test
    fun testReturnsProperContent() {
        runBlocking {
            val server = AndroidDebugBridgeServer()

            val client = server.startAndListen { input, output ->
                val transportCmd = input.receiveCommand()
                assertThat(transportCmd).isEqualTo("host:transport:serial")
                output.respond(Const.Message.OKAY)

                val shellCmd = input.receiveCommand()
                assertThat(shellCmd).isEqualTo("sync:")
                output.respond(Const.Message.OKAY)

                val receiveStat = input.receiveStatV2()
                assertThat(receiveStat).isEqualTo("/sdcard/testfile")

                output.respondStatV2(
                    mode = 123,
                    size = 420,
                    error = 0,
                    dev = 114,
                    ino = 111221,
                    nlink = 2,
                    uid = 0,
                    gid = 1000,
                    atime = 1589042331,
                    mtime = 1589042332,
                    ctime = 1589042333
                )
            }

            val result = client.execute(CompatStatFileRequest("/sdcard/testfile", listOf(Feature.STAT_V2)), serial = "serial")
            val output = result as FileEntryV2
            assertThat(output).isEqualTo(
                FileEntryV2(
                    mode = 123.toUInt(),
                    size = 420.toULong(),
                    error = 0.toUInt(),
                    dev = 114.toULong(),
                    ino = 111221.toULong(),
                    nlink = 2.toUInt(),
                    uid = 0.toUInt(),
                    gid = 1000.toUInt(),
                    atime = Instant.ofEpochSecond(1589042331),
                    mtime = Instant.ofEpochSecond(1589042332),
                    ctime = Instant.ofEpochSecond(1589042333)
                )
            )

            server.dispose()
        }
    }
}
