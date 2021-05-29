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

package com.malinskiy.adam.request.misc

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.malinskiy.adam.AndroidDebugBridgeClient
import com.malinskiy.adam.extension.toRequestString
import com.malinskiy.adam.server.junit4.AdbServerRule
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class RemountPartitionsRequestTest {
    @get:Rule
    val server = AdbServerRule()
    val client: AndroidDebugBridgeClient
        get() = server.client

    @Test
    fun testSerialize() {
        assertThat(RemountPartitionsRequest().serialize().toRequestString()).isEqualTo("0008remount:")
        assertThat(RemountPartitionsRequest(false).serialize().toRequestString()).isEqualTo("0008remount:")
    }

    @Test
    fun testSerializeAutoReboot() {
        assertThat(RemountPartitionsRequest(true).serialize().toRequestString()).isEqualTo("000Aremount:-R")
    }

    @Test
    fun testReturnsProperContent() {
        runBlocking {
            server.session {
                expectCmd { "host:transport:serial" }.accept()
                expectCmd { "remount:" }.accept()
                respondRemountPartitions { "something-something" }
            }

            val output = client.execute(RemountPartitionsRequest(), serial = "serial")
            assertThat(output).isEqualTo("something-something")
        }
    }
}
