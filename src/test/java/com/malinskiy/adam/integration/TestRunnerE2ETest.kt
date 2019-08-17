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

import com.malinskiy.adam.request.sync.InstallRemotePackageRequest
import com.malinskiy.adam.request.sync.PushFileRequest
import com.malinskiy.adam.request.testrunner.InstrumentOptions
import com.malinskiy.adam.request.testrunner.TestRunnerRequest
import com.malinskiy.adam.rule.AdbDeviceRule
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import java.io.File
import kotlin.math.roundToInt

class TestRunnerE2ETest {
    @get:Rule
    @JvmField
    val rule = AdbDeviceRule()

    @Test
    fun test1() {
        val apk = File(javaClass.getResource("/app-debug.apk").toURI())
        val testApk = File(javaClass.getResource("/app-debug-androidTest.apk").toURI())
        val apkFileName = apk.name
        val testApkFileName = testApk.name

        runBlocking {
            installApk(apk, apkFileName)
            installApk(testApk, testApkFileName)

            val channel = rule.adb.execute(
                TestRunnerRequest(
                    "com.example.test",
                    InstrumentOptions()
                ), serial = rule.deviceSerial,
                scope = GlobalScope
            )

            while (!channel.isClosedForReceive) {
                val logPart = channel.receiveOrNull() ?: break

                print(logPart)
            }
            println()
        }
    }

    private suspend fun installApk(apk: File, apkFileName: String) {
        val channel =
            rule.adb.execute(PushFileRequest(apk, "/data/local/tmp/$apkFileName"), GlobalScope, serial = rule.deviceSerial)

        var percentage = 0
        while (!channel.isClosedForReceive) {
            val percentageDouble = channel.receiveOrNull() ?: break

            val newPercentage = (percentageDouble * 100).roundToInt()
            if (newPercentage != percentage) {
                print('.')
                percentage = newPercentage
            }
        }
        println()

        val result = rule.adb.execute(
            InstallRemotePackageRequest("/data/local/tmp/$apkFileName", true),
            serial = rule.deviceSerial
        )
    }
}