/*
 * Copyright (C) 2019 Anton Malinskiy
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

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import com.malinskiy.adam.request.sync.*
import com.malinskiy.adam.rule.AdbDeviceRule
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import java.io.File

class ApkE2ETest {

    @get:Rule
    @JvmField
    val adb = AdbDeviceRule()
    val server = adb.adb

    @Test
    fun testScenario1() {
        runBlocking {
            val testFile = File(javaClass.getResource("/app-debug.apk").toURI())
            val fileName = testFile.name
            val channel =
                server.execute(PushFileRequest(testFile, "/data/local/tmp/$fileName"), GlobalScope, serial = adb.deviceSerial)

            while (!channel.isClosedForReceive) {
                channel.poll()
            }

            server.execute(InstallRemotePackageRequest("/data/local/tmp/$fileName", true), serial = adb.deviceSerial)

            var packages = server.execute(PmListRequest(), serial = adb.deviceSerial)
            assertThat(packages)
                .contains(Package("com.example"))

            server.execute(UninstallRemotePackageRequest("com.example"), adb.deviceSerial)

            packages = server.execute(PmListRequest(), serial = adb.deviceSerial)
            assertThat(packages)
                .doesNotContain(Package("com.example"))
        }
    }
}