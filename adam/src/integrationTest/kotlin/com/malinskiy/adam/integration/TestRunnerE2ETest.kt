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

import com.malinskiy.adam.request.pkg.InstallRemotePackageRequest
import com.malinskiy.adam.request.shell.v1.ShellCommandRequest
import com.malinskiy.adam.request.sync.v1.PushFileRequest
import com.malinskiy.adam.request.testrunner.InstrumentOptions
import com.malinskiy.adam.request.testrunner.TestEvent
import com.malinskiy.adam.request.testrunner.TestRunnerRequest
import com.malinskiy.adam.rule.AdbDeviceRule
import com.malinskiy.adam.rule.TestFixtures
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

class TestRunnerE2ETest {
    @Rule
    @JvmField
    val rule = AdbDeviceRule()

    @Before
    fun setup() {
        runBlocking {
            rule.adb.execute(ShellCommandRequest("rm /data/local/tmp/app-debug.apk"), rule.deviceSerial)
            rule.adb.execute(ShellCommandRequest("rm /data/local/tmp/app-debug-androidTest.apk"), rule.deviceSerial)
        }
    }

    @After
    fun teardown() {
        runBlocking {
            rule.adb.execute(ShellCommandRequest("rm /data/local/tmp/app-debug.apk"), rule.deviceSerial)
            rule.adb.execute(ShellCommandRequest("rm /data/local/tmp/app-debug-androidTest.apk"), rule.deviceSerial)
        }
    }

    @Test
    fun test1() {
        val apk = TestFixtures.apk("/app-debug.apk")
        val testApk = TestFixtures.apk("/app-debug-androidTest.apk")
        val apkFileName = apk.name
        val testApkFileName = testApk.name

        runBlocking {
            installApk(apk, apkFileName)
            installApk(testApk, testApkFileName)

            val channel = rule.adb.execute(
                TestRunnerRequest(
                    "com.example.test",
                    InstrumentOptions(
                        clazz = listOf("com.example.AbstractFailingTest")
                    )
                ), serial = rule.deviceSerial,
                scope = this
            )

            val events = mutableListOf<TestEvent>()
            for (list in channel) {
                events.addAll(list)
            }

            println(events)
        }
    }

    private suspend fun CoroutineScope.installApk(apk: File, apkFileName: String) {
        val channel =
            rule.adb.execute(
                PushFileRequest(apk, "/data/local/tmp/$apkFileName", coroutineContext = coroutineContext),
                this,
                serial = rule.deviceSerial
            )

        for (i in channel) {
            println(i)
        }

        val result = rule.adb.execute(
            InstallRemotePackageRequest("/data/local/tmp/$apkFileName", true),
            serial = rule.deviceSerial
        )
        println(result)
    }
}
