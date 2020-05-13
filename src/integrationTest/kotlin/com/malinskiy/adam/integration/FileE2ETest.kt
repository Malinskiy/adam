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
import kotlinx.coroutines.channels.receiveOrNull
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import java.io.File
import kotlin.math.roundToInt

class FileE2ETest {
    @get:Rule
    @JvmField
    val adbRule = AdbDeviceRule()

    private suspend fun md5(): String {
        var output = adbRule.adb.execute(ShellCommandRequest("ls /system/bin/md5"), adbRule.deviceSerial)
        var value = output.trim { it <= ' ' }
        if (!value.endsWith("No such file or directory")) {
            return "md5"
        }

        output = adbRule.adb.execute(ShellCommandRequest("ls /system/bin/md5sum"), adbRule.deviceSerial)
        value = output.trim { it <= ' ' }
        if (!value.endsWith("No such file or directory")) {
            return "md5sum"
        }

        throw RuntimeException("Android device should have md5 binary installed")
    }

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
            val stats = adbRule.adb.execute(StatFileRequest("/data/local/tmp/app-debug.apk"), adbRule.deviceSerial)
            assertThat(stats.size).isEqualTo(testFile.length().toInt())

            val sizeString = adbRule.adb.execute(ShellCommandRequest("${md5()} /data/local/tmp/app-debug.apk"), adbRule.deviceSerial)
            val split = sizeString.split(" ").filter { it != "" }

            /**
             * I've observed a behaviour with eventual consistency issue:
             * ls -ln returns a number lower than expected
             */
            assertThat(split[0]).isEqualTo(testFile.md5())

            //TODO figure out why 644 is actually pushed as 666
            assertThat(stats.mode).isEqualTo("100666".toInt(radix = 8))
        }
    }

    @Test
    fun testFilePulling() {
        runBlocking {
            val testFile = File("/tmp/manifest.xml")
            val channel = adbRule.adb.execute(
                PullFileRequest("/system/manifest.xml", testFile),
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

            /**
             * Some android ls implementations print the number of hard links
             * -rwxr-xr-x 0        2000       975152 2017-07-13 06:14 aapt
             * -rwxr-xr-x 1 0 2000     210 2019-04-13 06:23 am
             */
            val dateIndex = split.indexOfLast { it.matches("[\\d]{4}-[\\d]{2}-[\\d]{2}".toRegex()) }
            assertThat(split[dateIndex - 1].toLong()).isEqualTo(testFile.length())
        }
    }
}