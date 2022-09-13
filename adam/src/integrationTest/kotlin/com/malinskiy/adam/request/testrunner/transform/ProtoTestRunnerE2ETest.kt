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

package com.malinskiy.adam.request.testrunner.transform

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isTrue
import com.malinskiy.adam.request.pkg.InstallRemotePackageRequest
import com.malinskiy.adam.request.shell.v1.ShellCommandRequest
import com.malinskiy.adam.request.sync.v1.PushFileRequest
import com.malinskiy.adam.request.testrunner.InstrumentOptions
import com.malinskiy.adam.request.testrunner.TestEnded
import com.malinskiy.adam.request.testrunner.TestEvent
import com.malinskiy.adam.request.testrunner.TestFailed
import com.malinskiy.adam.request.testrunner.TestIdentifier
import com.malinskiy.adam.request.testrunner.TestRunEnded
import com.malinskiy.adam.request.testrunner.TestRunStartedEvent
import com.malinskiy.adam.request.testrunner.TestRunnerRequest
import com.malinskiy.adam.request.testrunner.TestStarted
import com.malinskiy.adam.rule.AdbDeviceRule
import com.malinskiy.adam.rule.TestFixtures
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

class ProtoTestRunnerE2ETest {
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
    fun testProto() {
        val apk = TestFixtures.apk("/app-debug.apk")
        val testApk = TestFixtures.apk("/app-debug-androidTest.apk")
        val apkFileName = apk.name
        val testApkFileName = testApk.name

        runBlocking {
            val output = rule.adb.execute(ShellCommandRequest("getprop ro.build.version.sdk"), rule.deviceSerial).output
            val sdk = output.trim().toIntOrNull() ?: 0
            Assume.assumeTrue("This device doesn't support proto output of am instrument", sdk >= 26)

            installApk(apk, apkFileName)
            installApk(testApk, testApkFileName)

            val channel = rule.adb.execute(
                TestRunnerRequest(
                    "com.example.test",

                    InstrumentOptions(
                        clazz = listOf("com.example.AbstractFailingTest")
                    ),
                    protobuf = true,
                    transformer = com.malinskiy.adam.request.testrunner.transform.ProtoInstrumentationResponseTransformer()
                ), serial = rule.deviceSerial,
                scope = this
            )

            val events = mutableListOf<TestEvent>()
            for (list in channel) {
                events.addAll(list)
            }

            assertThat(events).contains(TestRunStartedEvent(1))
            assertThat(events).contains(TestStarted(TestIdentifier("com.example.AbstractFailingTest", "testAlwaysFailing")))

            assertThat(events.any {
                it is TestFailed && it.id.className == "com.example.AbstractFailingTest" && it.id.testName == "testAlwaysFailing" && it.stackTrace.isNotEmpty()
            }).isTrue()

            assertThat(events.any {
                it is TestEnded && it.id.className == "com.example.AbstractFailingTest" && it.id.testName == "testAlwaysFailing"
            }).isTrue()

            assertThat(events.any {
                it is TestRunEnded && it.metrics.isEmpty() && it.elapsedTimeMillis > 0
            })
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
