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
import com.malinskiy.adam.request.Feature
import com.malinskiy.adam.request.misc.ExecInRequest
import com.malinskiy.adam.request.pkg.Package
import com.malinskiy.adam.request.pkg.PmListRequest
import com.malinskiy.adam.request.pkg.StreamingPackageInstallRequest
import com.malinskiy.adam.request.pkg.UninstallRemotePackageRequest
import com.malinskiy.adam.request.pkg.multi.InstallMultiPackageRequest
import com.malinskiy.adam.request.pkg.multi.SingleFileInstallationPackage
import com.malinskiy.adam.rule.AdbDeviceRule
import com.malinskiy.adam.rule.DeviceType
import io.ktor.util.cio.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import kotlin.system.measureTimeMillis

class CmdE2ETest {
    @Rule
    @JvmField
    val adb = AdbDeviceRule(DeviceType.ANY, Feature.CMD)
    val client = adb.adb

    @Before
    fun setup() {
        runBlocking {
            client.execute(UninstallRemotePackageRequest("com.example"), adb.deviceSerial)
            client.execute(UninstallRemotePackageRequest("com.example.test"), adb.deviceSerial)
        }
    }

    @After
    fun teardown() {
        runBlocking {
            client.execute(UninstallRemotePackageRequest("com.example"), adb.deviceSerial)
            client.execute(UninstallRemotePackageRequest("com.example.test"), adb.deviceSerial)
        }
    }

    @Test
    fun testStreaming() {
        runBlocking {
            measureTimeMillis {
                val testFile = File(javaClass.getResource("/app-debug.apk").toURI())
                val success = client.execute(
                    StreamingPackageInstallRequest(
                        pkg = testFile,
                        supportedFeatures = listOf(Feature.CMD),
                        reinstall = false
                    ),
                    adb.deviceSerial
                )
            }.let { println(it) }

            var packages = client.execute(PmListRequest(), serial = adb.deviceSerial)
            assertThat(packages)
                .contains(Package("com.example"))
        }
    }

    @Test
    fun testExecIn() {
        runBlocking {
            val testFile = File(javaClass.getResource("/app-debug.apk").toURI())
            val success = client.execute(
                ExecInRequest(
                    "cmd package install -S ${testFile.length()}",
                    testFile.readChannel()
                ),
                adb.deviceSerial
            )

            //Takes some time until it shows in the pm list. Wait for 10 seconds max
            var packages: List<Package> = emptyList()
            for (i in 1..100) {
                packages = client.execute(PmListRequest(), serial = adb.deviceSerial)
                if (packages.contains(Package("com.example"))) {
                    break
                }
                delay(100)
            }


            assertThat(packages)
                .contains(Package("com.example"))
        }
    }

    @Test
    fun testInstallMultiplePackageRequest() {
        runBlocking {
            val appFile = File(javaClass.getResource("/app-debug.apk").toURI())
            val testFile = File(javaClass.getResource("/app-debug-androidTest.apk").toURI())
            val success = client.execute(
                InstallMultiPackageRequest(
                    listOf(
                        SingleFileInstallationPackage(appFile),
                        SingleFileInstallationPackage(testFile)
                    ),
                    listOf(Feature.CMD),
                    true
                ),
                adb.deviceSerial
            )

            //Takes some time until it shows in the pm list. Wait for 10 seconds max
            var packages: List<Package> = emptyList()
            for (i in 1..100) {
                packages = client.execute(PmListRequest(), serial = adb.deviceSerial)
                if (packages.contains(Package("com.example")) && packages.contains(Package("com.example.test"))) {
                    break
                }
                delay(100)
            }


            assertThat(packages)
                .contains(Package("com.example"))
            assertThat(packages)
                .contains(Package("com.example.test"))
        }
    }
}
