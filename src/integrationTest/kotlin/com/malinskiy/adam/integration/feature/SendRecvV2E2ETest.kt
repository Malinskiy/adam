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
import com.malinskiy.adam.request.shell.v1.ShellCommandRequest
import com.malinskiy.adam.request.sync.v2.PullFileRequest
import com.malinskiy.adam.rule.AdbDeviceRule
import com.malinskiy.adam.rule.DeviceType
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.receiveOrNull
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.math.roundToInt

class SendRecvV2E2ETest {

    @Rule
    @JvmField
    val adbRule = AdbDeviceRule(DeviceType.ANY, Feature.SENDRECV_V2)

    @Rule
    @JvmField
    val temp = TemporaryFolder()

    @Before
    fun setup() {
        runBlocking {
            withTimeout(10_000) {
                while (true) {
                    var output =
                        adbRule.adb.execute(ShellCommandRequest("echo cafebabe > /data/local/tmp/testfile"), serial = adbRule.deviceSerial)
                    println(output)
                    output = adbRule.adb.execute(ShellCommandRequest("cat /data/local/tmp/testfile"), serial = adbRule.deviceSerial)
                    println(output)
                    if (output.output.contains("cafebabe") && output.exitCode == 0) {
                        break
                    }
                }
            }
        }
    }

    @After
    fun teardown() {
        runBlocking {
            adbRule.adb.execute(ShellCommandRequest("rm /data/local/tmp/testfile"), adbRule.deviceSerial)
        }
    }

    @Test
    fun testFilePulling() {
        runBlocking {
            val file = temp.newFile()

            val channel = adbRule.adb.execute(
                PullFileRequest("/data/local/tmp/testfile", file),
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

            assertThat(file.readText()).contains("cafebabe")
        }
    }
}
