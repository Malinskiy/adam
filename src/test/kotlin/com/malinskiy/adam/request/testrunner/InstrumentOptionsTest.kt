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
import assertk.assertions.containsExactly
import assertk.assertions.containsOnly
import assertk.assertions.isEqualTo
import org.junit.Test

class InstrumentOptionsTest {
    @Test
    fun testDefault() {
        assertThat(InstrumentOptions().toString()).isEqualTo("")
    }

    @Test
    fun testAll() {
        val options = InstrumentOptions(
            pkg = listOf("com.example"),
            clazz = listOf("com.example.TestClass"),
            overrides = mapOf("test" to "test"),
            coverageFile = "/sdcard/codecov",
            debug = true,
            emma = true,
            filterSize = InstrumentationSizeOption.MEDIUM,
            functional = true,
            log = true,
            performance = true,
            unit = true
        )

        assertThat(options.toString()).isEqualTo(
            " -e package com.example " +
                    "-e class com.example.TestClass " +
                    "-e func true " +
                    "-e unit true " +
                    "-e size medium " +
                    "-e perf true " +
                    "-e debug true " +
                    "-e log true " +
                    "-e emma true " +
                    "-e coverageFile /sdcard/codecov " +
                    "-e test test"
        )
    }

    @Test
    fun testAllWithProperties() {
        val options = InstrumentOptions(
            pkg = listOf("com.example"),
            clazz = listOf("com.example.TestClass"),
            overrides = mapOf("test" to "test"),
            coverageFile = "/sdcard/codecov",
            debug = true,
            emma = true,
            filterSize = InstrumentationSizeOption.MEDIUM,
            functional = true,
            log = true,
            performance = true,
            unit = true
        )

        assertThat(options.pkg).containsExactly("com.example")
        assertThat(options.clazz).containsExactly("com.example.TestClass")
        assertThat(options.overrides).containsOnly("test" to "test")
        assertThat(options.coverageFile).isEqualTo("/sdcard/codecov")
        assertThat(options.debug).isEqualTo(true)
        assertThat(options.emma).isEqualTo(true)
        assertThat(options.filterSize).isEqualTo(InstrumentationSizeOption.MEDIUM)
        assertThat(options.functional).isEqualTo(true)
        assertThat(options.log).isEqualTo(true)
        assertThat(options.performance).isEqualTo(true)
        assertThat(options.unit).isEqualTo(true)
    }
}