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
import assertk.assertions.isEqualTo
import com.malinskiy.adam.request.shell.v2.ShellV2CommandRequest
import com.malinskiy.adam.request.sync.Feature
import com.malinskiy.adam.rule.AdbDeviceRule
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class ShellV2E2ETest {
    @Rule
    @JvmField
    val adbRule = AdbDeviceRule(Feature.SHELL_V2)

    @Test
    fun testDefault() = runBlocking {
        val result = adbRule.adb.execute(ShellV2CommandRequest("echo foo; echo bar >&2; exit 17"), adbRule.deviceSerial)
        assertThat(result.exitCode).isEqualTo(17)
        assertThat(result.stdout).isEqualTo("foo\n")
        assertThat(result.stderr).isEqualTo("bar\n")
    }
}