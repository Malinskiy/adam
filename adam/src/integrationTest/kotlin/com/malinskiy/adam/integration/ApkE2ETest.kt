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
import com.malinskiy.adam.request.pkg.InstallRemotePackageRequest
import com.malinskiy.adam.request.pkg.InstallSplitPackageRequest
import com.malinskiy.adam.request.pkg.Package
import com.malinskiy.adam.request.pkg.PmListRequest
import com.malinskiy.adam.request.pkg.UninstallRemotePackageRequest
import com.malinskiy.adam.request.pkg.multi.ApkSplitInstallationPackage
import com.malinskiy.adam.request.shell.v1.ShellCommandRequest
import com.malinskiy.adam.request.sync.v1.PushFileRequest
import com.malinskiy.adam.rule.AdbDeviceRule
import kotlinx.coroutines.debug.junit4.CoroutinesTimeout
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import kotlin.system.measureTimeMillis

class ApkE2ETest {

    @Rule
    @JvmField
    val adb = AdbDeviceRule()
    val client = adb.adb

    @Rule
    @JvmField
    val timeout = CoroutinesTimeout.seconds(60)

    @Before
    fun setup() {
        runBlocking {
            client.execute(UninstallRemotePackageRequest("com.example"), adb.deviceSerial)
            client.execute(ShellCommandRequest("rm /data/local/tmp/app-debug.apk"), adb.deviceSerial)
        }
    }

    @After
    fun teardown() {
        runBlocking {
            client.execute(UninstallRemotePackageRequest("com.example"), adb.deviceSerial)
            client.execute(ShellCommandRequest("rm /data/local/tmp/app-debug.apk"), adb.deviceSerial)
        }
    }

    @Test
    fun testScenario1() {
        runBlocking {
            measureTimeMillis {
                val testFile = File(javaClass.getResource("/app-debug.apk").toURI())
                val fileName = testFile.name
                val channel =
                    client.execute(
                        PushFileRequest(testFile, "/data/local/tmp/$fileName", coroutineContext = coroutineContext),
                        this,
                        serial = adb.deviceSerial
                    )

                for (result in channel) {
                    //Do something with result
                }

                client.execute(InstallRemotePackageRequest("/data/local/tmp/$fileName", true), serial = adb.deviceSerial).let {
                    println(it)
                }
            }.let { println(it) }

            var packages = client.execute(PmListRequest(), serial = adb.deviceSerial)
            assertThat(packages)
                .contains(Package("com.example"))

            client.execute(UninstallRemotePackageRequest("com.example"), adb.deviceSerial)

            packages = client.execute(PmListRequest(), serial = adb.deviceSerial)
            assertThat(packages)
                .doesNotContain(Package("com.example"))
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
                    listOf(),
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

    @Test
    fun testApkSplitInstallWithExtraArgs() {
        runBlocking {
            val appFile1 = File(javaClass.getResource("/split/base-en.apk").toURI())
            val appFile2 = File(javaClass.getResource("/split/standalone-hdpi.apk").toURI())
            client.execute(
                InstallSplitPackageRequest(
                    ApkSplitInstallationPackage(appFile1, appFile2),
                    listOf(),
                    true,
                    extraArgs = listOf("-g")
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
