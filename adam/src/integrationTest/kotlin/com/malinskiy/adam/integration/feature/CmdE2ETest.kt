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
import com.malinskiy.adam.exception.RequestRejectedException
import com.malinskiy.adam.extension.sequentialRead
import com.malinskiy.adam.request.Feature
import com.malinskiy.adam.request.misc.ExecInRequest
import com.malinskiy.adam.request.pkg.AtomicInstallPackageRequest
import com.malinskiy.adam.request.pkg.InstallSplitPackageRequest
import com.malinskiy.adam.request.pkg.Package
import com.malinskiy.adam.request.pkg.PmListRequest
import com.malinskiy.adam.request.pkg.StreamingPackageInstallRequest
import com.malinskiy.adam.request.pkg.UninstallRemotePackageRequest
import com.malinskiy.adam.request.pkg.multi.ApkSplitInstallationPackage
import com.malinskiy.adam.request.pkg.multi.SingleFileInstallationPackage
import com.malinskiy.adam.rule.AdbDeviceRule
import com.malinskiy.adam.rule.DeviceType
import com.malinskiy.adam.rule.TestFixtures
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assume
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
                val testFile = TestFixtures.apk("/app-debug.apk")
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
            val testFile = TestFixtures.apk("/app-debug.apk")

            val blockSizeChannel = Channel<Int>(capacity = 1)
            val channel: ReceiveChannel<ByteArray> = sequentialRead(testFile, blockSizeChannel)
            val success = client.execute(
                ExecInRequest(
                    "cmd package install -S ${testFile.length()}",
                    channel,
                    blockSizeChannel
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
            val appFile = TestFixtures.apk("/app-debug.apk")
            val testFile = TestFixtures.apk("/app-debug-androidTest.apk")
            try {
                val success = client.execute(
                    AtomicInstallPackageRequest(
                        listOf(
                            SingleFileInstallationPackage(appFile),
                            SingleFileInstallationPackage(testFile)
                        ),
                        listOf(Feature.CMD),
                        true
                    ),
                    adb.deviceSerial
                )
            } catch (e: RequestRejectedException) {
                Assume.assumeTrue(
                    "Device doesn't support `--multi-package` option",
                    e.message?.contains("Unknown option --multi-package") == true
                )
                throw e
            }

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

    @Test
    fun testApkSplitInstall() {
        runBlocking {
            val appFile1 = File(javaClass.getResource("/split/base-en.apk").toURI())
            val appFile2 = File(javaClass.getResource("/split/standalone-hdpi.apk").toURI())
            client.execute(
                InstallSplitPackageRequest(
                    ApkSplitInstallationPackage(appFile1, appFile2),
                    listOf(Feature.CMD),
                    true
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
}
