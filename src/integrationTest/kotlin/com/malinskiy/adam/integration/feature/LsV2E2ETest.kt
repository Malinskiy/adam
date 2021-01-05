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

package com.malinskiy.adam.integration.feature

import assertk.assertThat
import com.malinskiy.adam.request.Feature
import com.malinskiy.adam.request.shell.v1.ShellCommandRequest
import com.malinskiy.adam.request.sync.v2.ListFileRequest
import com.malinskiy.adam.rule.AdbDeviceRule
import com.malinskiy.adam.rule.DeviceType
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class LsV2E2ETest {
    @Rule
    @JvmField
    val adbRule = AdbDeviceRule(DeviceType.ANY, Feature.LS_V2)
    lateinit var externalStorageMount: String

    @Before
    fun setup() {
        runBlocking {
            externalStorageMount = adbRule.adb.execute(ShellCommandRequest("echo \$EXTERNAL_STORAGE"), adbRule.deviceSerial).output.trim()
        }
    }

    @Test
    fun testListFile() {
        runBlocking {
            val list = adbRule.adb.execute(ListFileRequest(externalStorageMount), adbRule.deviceSerial)
            for (i in list) {
                println(i)
            }

            assertThat(list.any { it.name == "." })
        }
    }
}
