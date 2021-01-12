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
import assertk.assertions.startsWith
import com.malinskiy.adam.request.Feature
import com.malinskiy.adam.request.abb.AbbRequest
import com.malinskiy.adam.rule.AdbDeviceRule
import com.malinskiy.adam.rule.DeviceType
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class AbbE2ETest {
    @Rule
    @JvmField
    val adbRule = AdbDeviceRule(DeviceType.ANY, Feature.ABB)

    @Test
    fun testStreamingInstallRequest() {
        runBlocking {
            var result = adbRule.adb.execute(AbbRequest(listOf("-l"), adbRule.supportedFeatures), serial = adbRule.deviceSerial)
            assertThat(result.stdout).startsWith("Currently running services:")
        }
    }
}
