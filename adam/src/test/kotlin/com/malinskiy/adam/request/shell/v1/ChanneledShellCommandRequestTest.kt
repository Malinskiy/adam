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

package com.malinskiy.adam.request.shell.v1

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.malinskiy.adam.AndroidDebugBridgeClient
import com.malinskiy.adam.server.junit4.AdbServerRule
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class ChanneledShellCommandRequestTest {
    @get:Rule
    val server = AdbServerRule()
    val client: AndroidDebugBridgeClient
        get() = server.client

    @Test
    fun testReturnsProperContent() {
        runBlocking {
            server.session {
                expectCmd { "host:transport:emulator-5554" }.accept()
                expectShell { "logcat -v" }
                    .accept()
                    .respond("something-something")
                    .respond("something2-something2")
            }

            val updates = client.execute(ChanneledShellCommandRequest("logcat -v"), scope = this, serial = "emulator-5554")
            val stringBuffer = StringBuffer()

            for (update in updates) {
                stringBuffer.append(update)
            }

            assertThat(stringBuffer.toString()).isEqualTo("something-somethingsomething2-something2")
        }
    }
}
