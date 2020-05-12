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

package com.malinskiy.adam.request.testrunner

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.malinskiy.adam.Const
import org.junit.Test

class TestRunnerRequestTest {
    @Test
    fun testSerialize() {
        val request = TestRunnerRequest(
            testPackage = "com.example.test",
            noWindowAnimations = true,
            instrumentOptions = InstrumentOptions(),
            abi = "x86",
            noHiddenApiChecks = true,
            outputLogPath = "/sdcard/log",
            profilingOutputPath = "/sdcard/profiling",
            runnerClass = "com.example.test.AndroidTestRunner",
            userId = 1000
        )

        assertThat(String(request.serialize(), Const.DEFAULT_TRANSPORT_ENCODING))
            .isEqualTo(
                "00B4shell:am instrument -w -r --no-hidden-api-checks --no-window-animation --user 1000 --abi x86 " +
                        "-p /sdcard/profiling -f /sdcard/log com.example.test/com.example.test.AndroidTestRunner"
            )
    }

    @Test
    fun testDefaultSerialize() {
        val request = TestRunnerRequest(
            testPackage = "com.example.test",
            instrumentOptions = InstrumentOptions()
        )

        assertThat(String(request.serialize(), Const.DEFAULT_TRANSPORT_ENCODING))
            .isEqualTo(
                "0059shell:am instrument -w -r com.example.test/android.support.test.runner.AndroidJUnitRunner"
            )
    }
}