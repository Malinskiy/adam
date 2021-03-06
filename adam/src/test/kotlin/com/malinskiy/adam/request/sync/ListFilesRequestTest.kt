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
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.malinskiy.adam.AndroidDebugBridgeClient
import com.malinskiy.adam.server.junit4.AdbServerRule
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class ListFilesRequestTest {
    @get:Rule
    val server = AdbServerRule()
    val client: AndroidDebugBridgeClient
        get() = server.client

    @Test
    fun testReturnsProperContent() {
        runBlocking {
            server.session {
                expectCmd { "host:transport:serial" }.accept()
                expectShell { "ls -l /sdcard/;echo x$?" }
                    .accept()
                    .respond(
                        """
                    total 88
                    -rwxrwx--x 2 root sdcard_rw 4096 2020-10-24 16:29 Alarms
                    brwxrwx--x 4 root sdcard_rw 4096 2020-10-24 16:29 Android
                    lrwxrwx--x 2 root sdcard_rw 4096 2020-10-24 16:29 DCIM -> ../Camera
                    crwxrwx--x 2 root sdcard_rw 4096 2020-12-01 19:11 Download
                    srwxrwx--x 2 root sdcard_rw 4096 2020-10-24 16:29 Movies
                    prwxrwx--x 2 root sdcard_rw 4096 2020-10-24 16:29 Music
                    drwxrwx--x 2 root sdcard_rw 4096 2020-10-24 16:29 Ringtones
                    Orwxrwx--x 2 root sdcard_rw 4096 2020-10-24 16:29 XXX
                    x0
                """.trimIndent()
                    )
            }

            val files = client.execute(ListFilesRequest("/sdcard/"), serial = "serial")
            assertThat(files).containsExactly(
                AndroidFile(
                    permissions = "-rwxrwx--x",
                    directory = "/sdcard/",
                    date = "2020-10-24",
                    group = "sdcard_rw",
                    link = null,
                    name = "Alarms",
                    owner = "root",
                    size = 4096,
                    time = "16:29",
                    type = AndroidFileType.REGULAR_FILE
                ),
                AndroidFile(
                    permissions = "brwxrwx--x",
                    directory = "/sdcard/",
                    date = "2020-10-24",
                    group = "sdcard_rw",
                    link = null,
                    name = "Android",
                    owner = "root",
                    size = 4096,
                    time = "16:29",
                    type = AndroidFileType.BLOCK_SPECIAL_FILE
                ),
                AndroidFile(
                    permissions = "lrwxrwx--x",
                    directory = "/sdcard/",
                    date = "2020-10-24",
                    group = "sdcard_rw",
                    link = "../Camera",
                    name = "DCIM",
                    owner = "root",
                    size = 4096,
                    time = "16:29",
                    type = AndroidFileType.SYMBOLIC_LINK
                ),
                AndroidFile(
                    permissions = "crwxrwx--x",
                    directory = "/sdcard/",
                    date = "2020-12-01",
                    group = "sdcard_rw",
                    link = null,
                    name = "Download",
                    owner = "root",
                    size = 4096,
                    time = "19:11",
                    type = AndroidFileType.CHARACTER_SPECIAL_FILE
                ),
                AndroidFile(
                    permissions = "srwxrwx--x",
                    directory = "/sdcard/",
                    date = "2020-10-24",
                    group = "sdcard_rw",
                    link = null,
                    name = "Movies",
                    owner = "root",
                    size = 4096,
                    time = "16:29",
                    type = AndroidFileType.SOCKET_LINK
                ),
                AndroidFile(
                    permissions = "prwxrwx--x",
                    directory = "/sdcard/",
                    date = "2020-10-24",
                    group = "sdcard_rw",
                    link = null,
                    name = "Music",
                    owner = "root",
                    size = 4096,
                    time = "16:29",
                    type = AndroidFileType.FIFO
                ),
                AndroidFile(
                    permissions = "drwxrwx--x",
                    directory = "/sdcard/",
                    date = "2020-10-24",
                    group = "sdcard_rw",
                    link = null,
                    name = "Ringtones",
                    owner = "root",
                    size = 4096,
                    time = "16:29",
                    type = AndroidFileType.DIRECTORY
                )
            )

            assertThat(files.first().permissions).isEqualTo("-rwxrwx--x")
            assertThat(files.first().directory).isEqualTo("/sdcard/")
            assertThat(files.first().date).isEqualTo("2020-10-24")
            assertThat(files.first().group).isEqualTo("sdcard_rw")
            assertThat(files.first().link).isNull()
            assertThat(files.first().name).isEqualTo("Alarms")
            assertThat(files.first().owner).isEqualTo("root")
            assertThat(files.first().size).isEqualTo(4096L)
            assertThat(files.first().time).isEqualTo("16:29")
            assertThat(files.first().type).isEqualTo(AndroidFileType.REGULAR_FILE)
        }
    }
}
