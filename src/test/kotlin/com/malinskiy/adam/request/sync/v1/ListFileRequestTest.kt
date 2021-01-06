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

package com.malinskiy.adam.request.sync.v1

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import com.malinskiy.adam.Const
import com.malinskiy.adam.request.sync.model.FileEntryV1
import com.malinskiy.adam.server.AndroidDebugBridgeServer
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.time.Instant

class ListFileRequestTest {
    @Test
    fun testSerialize() = runBlocking {
        val server = AndroidDebugBridgeServer()

        val client = server.startAndListen { input, output ->
            val transportCmd = input.receiveCommand()
            assertThat(transportCmd).isEqualTo("host:transport:serial")
            output.respond(Const.Message.OKAY)

            val actualCommand = input.receiveCommand()
            assertThat(actualCommand).isEqualTo("sync:")
            output.respond(Const.Message.OKAY)

            val listPath = input.receiveList()
            assertThat(listPath).isEqualTo("/sdcard/")
            output.respondList(
                420,
                123,
                1589042331,
                "some-file"
            )
            output.respondDone()
        }

        val list = client.execute(
            ListFileRequest("/sdcard/"), "serial"
        )


        server.dispose()

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
