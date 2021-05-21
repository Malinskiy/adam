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

package com.malinskiy.adam.request.pkg

import assertk.assertThat
import assertk.assertions.containsExactly
import com.malinskiy.adam.AndroidDebugBridgeClient
import com.malinskiy.adam.server.junit4.AdbServerRule
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class PmListRequestTest {
    @get:Rule
    val server = AdbServerRule()
    val client: AndroidDebugBridgeClient
        get() = server.client

    @Test
    fun testReturnsProperContent() {
        runBlocking {
            server.session {
                expectCmd { "host:transport:serial" }.accept()
                expectShell { "pm list packages;echo x$?" }
                    .accept()
                    .respond("package:test.packagex0")
            }

            val output = client.execute(PmListRequest(), serial = "serial")
            assertThat(output).containsExactly(Package("test.package"))
        }
    }

    @Test
    fun testReturnsProperContentWithPath() {
        runBlocking {
            server.session {
                expectCmd { "host:transport:serial" }.accept()
                expectShell { "pm list packages -f;echo x$?" }
                    .accept()
                    .respond("package:/data/app/x=test.packagex0\n\n")
            }

            val output = client.execute(PmListRequest(includePath = true), serial = "serial")
            assertThat(output).containsExactly(Package("test.package", "/data/app/x"))
        }
    }
}
