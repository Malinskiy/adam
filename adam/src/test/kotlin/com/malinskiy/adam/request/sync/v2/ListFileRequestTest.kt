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
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import com.malinskiy.adam.AndroidDebugBridgeClient
import com.malinskiy.adam.request.Feature
import com.malinskiy.adam.request.sync.model.FileEntryV2
import com.malinskiy.adam.server.junit4.AdbServerRule
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class ListFileRequestTest {
    @get:Rule
    val server = AdbServerRule()
    val client: AndroidDebugBridgeClient
        get() = server.client

    @Test
    fun testSerialize() = runBlocking {
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
            )
            output.respondDone()
        }

        val list = client.execute(
            ListFileRequest("/sdcard/", listOf(Feature.LS_V2)), "serial"
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

        assertThat(list.first().name).isEqualTo("some-file")
        assertThat(list.first().mode).isEqualTo(123.toUInt())
        assertThat(list.first().size).isEqualTo(420.toULong())
        assertThat(list.first().error).isEqualTo(0.toUInt())
        assertThat(list.first().dev).isEqualTo(114.toULong())
        assertThat(list.first().ino).isEqualTo(111221.toULong())
        assertThat(list.first().nlink).isEqualTo(2.toUInt())
        assertThat(list.first().uid).isEqualTo(0.toUInt())
        assertThat(list.first().gid).isEqualTo(1000.toUInt())
        assertThat(list.first().atime).isEqualTo(Instant.ofEpochSecond(1589042331))
        assertThat(list.first().mtime).isEqualTo(Instant.ofEpochSecond(1589042332))
        assertThat(list.first().ctime).isEqualTo(Instant.ofEpochSecond(1589042333))
    }
}
