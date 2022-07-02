/*
 * Copyright (C) 2022 Anton Malinskiy
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

package com.malinskiy.adam.integration

import com.malinskiy.adam.request.logcat.SyncLogcatRequest
import com.malinskiy.adam.rule.AdbDeviceRule
import kotlinx.coroutines.debug.junit4.CoroutinesTimeout
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class LogcatE2ETest {
    @Rule
    @JvmField
    val adb = AdbDeviceRule()
    val client = adb.adb

    @Rule
    @JvmField
    val timeout = CoroutinesTimeout.seconds(60)

    private val instant = Instant.parse("2022-07-02T07:41:07Z")

    @Test
    fun testSyncRequest() {
        runBlocking {
            val content = client.execute(SyncLogcatRequest(since = instant))
            println(content)
        }
    }
}