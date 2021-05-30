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
import assertk.assertions.containsExactly
import com.malinskiy.adam.AndroidDebugBridgeClient
import com.malinskiy.adam.request.Feature
import com.malinskiy.adam.request.sync.model.FileEntryV1
import com.malinskiy.adam.request.sync.model.FileEntryV2
import com.malinskiy.adam.server.junit4.AdbServerRule
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class CompatListFileRequestTest {
    @get:Rule
    val server = AdbServerRule()
    val client: AndroidDebugBridgeClient
        get() = server.client

    @Test
    fun testV1() {
        runBlocking {
            server.session {
                expectCmd { "host:transport:serial" }.accept()
                expectCmd { "sync:" }.accept()

                expectList { "/sdcard/" }
                respondList(
                    420,
                    123,
                    1589042331,
                    "some-file"
                ).done()
            }

            val list = client.execute(
                CompatListFileRequest("/sdcard/", emptyList()), "serial"
            )

            assertThat(list).containsExactly(
                FileEntryV1(
                    name = "some-file",
                    mode = 123.toUInt(),
                    mtime = Instant.ofEpochSecond(1589042331),
                    size = 420.toUInt()
                )
            )
        }
    }

    @Test
    fun testV2() = runBlocking {
        server.session {
            expectCmd { "host:transport:serial" }.accept()
            expectCmd { "sync:" }.accept()

            expectListV2 { "/sdcard/" }
            respondListV2(
                name = "some-file",
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
            ).done()
        }

        val list = client.execute(
            CompatListFileRequest("/sdcard/", listOf(Feature.LS_V2)), "serial"
        )

        assertThat(list).containsExactly(
            FileEntryV2(
                name = "some-file",
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
    }
}
