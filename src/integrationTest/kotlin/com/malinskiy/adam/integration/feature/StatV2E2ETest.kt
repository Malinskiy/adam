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
import assertk.assertions.isEqualTo
import com.malinskiy.adam.request.Feature
import com.malinskiy.adam.request.sync.v1.PushFileRequest
import com.malinskiy.adam.request.sync.v2.StatFileRequest
import com.malinskiy.adam.rule.AdbDeviceRule
import com.malinskiy.adam.rule.DeviceType
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.receiveOrNull
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import kotlin.math.roundToInt

class StatV2E2ETest {
    @Rule
    @JvmField
    val adbRule = AdbDeviceRule(DeviceType.ANY, Feature.LS_V2)
    val testFile = File(javaClass.getResource("/app-debug.apk").toURI())

    @Before
    fun setup() {
        runBlocking {
            val fileName = testFile.name
            val channel =
                adbRule.adb.execute(PushFileRequest(testFile, "/data/local/tmp/$fileName"), GlobalScope, serial = adbRule.deviceSerial)

            var percentage = 0
            while (!channel.isClosedForReceive) {
                val percentageDouble = channel.receiveOrNull() ?: break

                val newPercentage = (percentageDouble * 100).roundToInt()
                if (newPercentage != percentage) {
                    print('.')
                    percentage = newPercentage
                }
            }
        }
    }

    @Test
    fun testListFile() {
        runBlocking {
            val stats = adbRule.adb.execute(StatFileRequest("/data/local/tmp/app-debug.apk"), adbRule.deviceSerial)
            assertThat(stats.size).isEqualTo(testFile.length().toUInt())
        }
    }
}
