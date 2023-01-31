/*
 * Copyright (C) 2020 Anton Malinskiy
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
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.malinskiy.adam.Const
import com.malinskiy.adam.request.Feature
import com.malinskiy.adam.request.shell.v2.ChanneledShellCommandRequest
import com.malinskiy.adam.request.shell.v2.ShellCommandInputChunk
import com.malinskiy.adam.request.shell.v2.ShellCommandRequest
import com.malinskiy.adam.rule.AdbDeviceRule
import com.malinskiy.adam.rule.DeviceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class ShellV2E2ETest {
    @Rule
    @JvmField
    val adbRule = AdbDeviceRule(DeviceType.ANY, Feature.SHELL_V2)

    @Test
    fun testDefault() = runBlocking {
        val result = adbRule.adb.execute(ShellCommandRequest("echo foo; echo bar >&2; exit 17"), adbRule.deviceSerial)
        assertThat(result.exitCode).isEqualTo(17)
        assertThat(result.stdout).isEqualTo("foo\n")
        assertThat(result.stderr).isEqualTo("bar\n")
    }

    @Test
    fun testChanneled() = runBlocking {
        val stdio = Channel<ShellCommandInputChunk>()
        val receiveChannel = adbRule.adb.execute(ChanneledShellCommandRequest("cat", stdio), this, adbRule.deviceSerial)
        //Sending commands requires additional pool, otherwise we might deadlock
        val stdioJob = launch(Dispatchers.IO) {
            stdio.send(
                ShellCommandInputChunk(
                    stdin = "cafebabe".toByteArray(Const.DEFAULT_TRANSPORT_ENCODING)
                )
            )

            stdio.send(
                ShellCommandInputChunk(
                    close = true
                )
            )
        }

        val stdoutBuilder = StringBuilder()
        val stderrBuilder = StringBuilder()
        var exitCode = 1
        for (i in receiveChannel) {
            i.stdout?.let { stdoutBuilder.append(it) }
            i.stderr?.let { stderrBuilder.append(it) }
            i.exitCode?.let { exitCode = it }
        }
        stdioJob.join()

        assertThat(stdoutBuilder.toString()).isEqualTo("cafebabe")
        assertThat(stderrBuilder.toString()).isEmpty()
        assertThat(exitCode).isEqualTo(0)

    }
}
