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
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import com.malinskiy.adam.request.Feature
import com.malinskiy.adam.request.pkg.Package
import com.malinskiy.adam.request.pkg.PmListRequest
import com.malinskiy.adam.request.pkg.StreamingPackageInstallRequest
import com.malinskiy.adam.request.pkg.UninstallRemotePackageRequest
import com.malinskiy.adam.rule.AdbDeviceRule
import com.malinskiy.adam.rule.DeviceType
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import java.io.File
import kotlin.system.measureTimeMillis

class AbbExecE2ETest {
    @Rule
    @JvmField
    val adbRule = AdbDeviceRule(DeviceType.ANY, Feature.ABB_EXEC)

    @Test
    fun testStreamingInstallRequest() {
        runBlocking {
            measureTimeMillis {
                val testFile = File(javaClass.getResource("/app-debug.apk").toURI())
                val success = adbRule.adb.execute(
                    StreamingPackageInstallRequest(
                        pkg = testFile,
                        supportedFeatures = listOf(Feature.ABB_EXEC),
                        reinstall = false
                    ),
                    adbRule.deviceSerial
                )
            }.let { println(it) }

            var packages = adbRule.adb.execute(PmListRequest(), serial = adbRule.deviceSerial)
            assertThat(packages)
                .contains(Package("com.example"))

            adbRule.adb.execute(UninstallRemotePackageRequest("com.example"), adbRule.deviceSerial)

            packages = adbRule.adb.execute(PmListRequest(), serial = adbRule.deviceSerial)
            assertThat(packages)
                .doesNotContain(Package("com.example"))
        }
    }
}
