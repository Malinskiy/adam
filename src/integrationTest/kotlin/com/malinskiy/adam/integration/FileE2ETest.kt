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
import assertk.assertions.isEqualTo
import com.malinskiy.adam.extension.md5
import com.malinskiy.adam.request.sync.PullFileRequest
import com.malinskiy.adam.request.sync.PushFileRequest
import com.malinskiy.adam.request.sync.ShellCommandRequest
import com.malinskiy.adam.request.sync.StatFileRequest
import com.malinskiy.adam.rule.AdbDeviceRule
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import java.io.File
import kotlin.math.roundToInt

class FileE2ETest {
    @get:Rule
    @JvmField
    val adbRule = AdbDeviceRule()

    @Test
    fun testApkPushing() {
        val testFile = File(javaClass.getResource("/app-debug.apk").toURI())
        val fileName = testFile.name
        runBlocking {
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
            println()

            for (i in 1..10) {
                val stats = adbRule.adb.execute(StatFileRequest("/data/local/tmp/app-debug.apk"), adbRule.deviceSerial)
                if (stats.size == testFile.length().toInt()) break
                delay(100)
            }

            val sizeString = adbRule.adb.execute(ShellCommandRequest("md5 /data/local/tmp/app-debug.apk"), adbRule.deviceSerial)
            val split = sizeString.split(" ").filter { it != "" }

            /**
             * I've observed a behaviour with eventual consistency issue:
             * ls -ln returns a number lower than expected
             */
            assertThat(split[0]).isEqualTo(testFile.md5())

            val stats = adbRule.adb.execute(StatFileRequest("/data/local/tmp/app-debug.apk"), adbRule.deviceSerial)

            assertThat(stats.size).isEqualTo(testFile.length().toInt())
            //TODO figure out why 644 is actually pushed as 666
            assertThat(stats.mode).isEqualTo("100666".toInt(radix = 8))
        }
    }

    @Test
    fun testFilePulling() {
        runBlocking {
            val testFile = File("/tmp/build.prop")
            val channel = adbRule.adb.execute(
                PullFileRequest("/system/build.prop", testFile),
                GlobalScope,
                adbRule.deviceSerial
            )

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

            val sizeString = adbRule.adb.execute(ShellCommandRequest("ls -ln /system/build.prop"), adbRule.deviceSerial)
            val split = sizeString.split(" ").filter { it != "" }
            assertThat(split[3].toLong()).isEqualTo(testFile.length())
        }
    }
}