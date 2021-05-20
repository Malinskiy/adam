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

package com.malinskiy.adam.request.testrunner.transform

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import com.malinskiy.adam.Const
import com.malinskiy.adam.request.testrunner.TestEvent
import com.malinskiy.adam.request.testrunner.TestRunFailed
import com.malinskiy.adam.request.transform.InstrumentationResponseTransformer
import kotlinx.coroutines.runBlocking
import org.junit.Test

class InstrumentationResponseTransformerTest {

    @Test
    fun testSingleFailure() {
        runBlocking {
            val transformer = InstrumentationResponseTransformer()

            val lines = javaClass.getResourceAsStream("/instrumentation/log_3.input").reader().readLines()

            val events = mutableListOf<TestEvent>()
            for (line in lines) {
                val bytes = (line + '\n').toByteArray(Const.DEFAULT_TRANSPORT_ENCODING)
                transformer.process(bytes, 0, bytes.size)?.let {
                    events.addAll(it)
                }
            }
            transformer.transform()?.let { events.addAll(it) }

            assertThat(events.map { it.toString() }.reduce { acc, s -> acc + "\n" + s })
                .isEqualTo(javaClass.getResourceAsStream("/instrumentation/log_3.expected").reader().readText())
        }
    }

    @Test
    fun testComplex1() {
        runBlocking {
            val transformer = InstrumentationResponseTransformer()

            val lines = javaClass.getResourceAsStream("/instrumentation/log_2.input").reader().readLines()

            val events = mutableListOf<TestEvent>()
            for (line in lines) {
                val bytes = (line + '\n').toByteArray(Const.DEFAULT_TRANSPORT_ENCODING)
                transformer.process(bytes, 0, bytes.size)?.let {
                    events.addAll(it)
                }
            }
            transformer.transform()?.let { events.addAll(it) }

            assertThat(events.map { it.toString() }.reduce { acc, s -> acc + "\n" + s })
                .isEqualTo(javaClass.getResourceAsStream("/instrumentation/log_2.expected").reader().readText())
        }
    }

    @Test
    fun testComplex2() {
        runBlocking {
            val transformer = InstrumentationResponseTransformer()

            val lines = javaClass.getResourceAsStream("/instrumentation/log_1.input").reader().readLines()

            val events = mutableListOf<TestEvent>()
            for (line in lines) {
                val bytes = (line + '\n').toByteArray(Const.DEFAULT_TRANSPORT_ENCODING)
                transformer.process(bytes, 0, bytes.size)?.let {
                    events.addAll(it)
                }
            }
            transformer.transform()?.let { events.addAll(it) }

            assertThat(events.map { it.toString() }.reduce { acc, s -> acc + "\n" + s })
                .isEqualTo(javaClass.getResourceAsStream("/instrumentation/log_1.expected").reader().readText())
        }
    }

    @Test
    fun testNoResultsReported() {
        runBlocking {
            val transformer = InstrumentationResponseTransformer()
            val list = transformer.transform()
            assertThat(list!!).containsExactly(TestRunFailed("No test results"))
        }
    }

    @Test
    fun testIncompleteTests() {
        runBlocking {
            val transformer = InstrumentationResponseTransformer()

            val lines = javaClass.getResourceAsStream("/instrumentation/log_4.input").reader().readLines()

            val events = mutableListOf<TestEvent>()
            for (line in lines) {
                val bytes = (line + '\n').toByteArray(Const.DEFAULT_TRANSPORT_ENCODING)
                transformer.process(bytes, 0, bytes.size)?.let {
                    events.addAll(it)
                }
            }
            transformer.transform()?.let { events.addAll(it) }

            assertThat(events.map { it.toString() }.reduce { acc, s -> acc + "\n" + s })
                .isEqualTo(javaClass.getResourceAsStream("/instrumentation/log_4.expected").reader().readText())
        }
    }

    @Test
    fun testIncompleteTests2() {
        runBlocking {
            val transformer = InstrumentationResponseTransformer()

            val lines = javaClass.getResourceAsStream("/instrumentation/log_5.input").reader().readLines()

            val events = mutableListOf<TestEvent>()
            for (line in lines) {
                val bytes = (line + '\n').toByteArray(Const.DEFAULT_TRANSPORT_ENCODING)
                transformer.process(bytes, 0, bytes.size)?.let {
                    events.addAll(it)
                }
            }
            transformer.transform()?.let { events.addAll(it) }

            assertThat(events.map { it.toString() }.reduce { acc, s -> acc + "\n" + s })
                .isEqualTo(javaClass.getResourceAsStream("/instrumentation/log_5.expected").reader().readText())
        }
    }

    /**
     * This is purely theoretical scenario since this hasn't been observed in practice but is possible
     */
    @Test
    fun testOtherFailure() = runBlocking {
        val transformer = InstrumentationResponseTransformer()

        val lines = javaClass.getResourceAsStream("/instrumentation/log_6.input").reader().readLines()

        val events = mutableListOf<TestEvent>()
        for (line in lines) {
            val bytes = (line + '\n').toByteArray(Const.DEFAULT_TRANSPORT_ENCODING)
            transformer.process(bytes, 0, bytes.size)?.let {
                events.addAll(it)
            }
        }
        transformer.transform()?.let { events.addAll(it) }

        assertThat(events.map { it.toString() }.reduce { acc, s -> acc + "\n" + s })
            .isEqualTo(javaClass.getResourceAsStream("/instrumentation/log_6.expected").reader().readText())
    }

    @Test
    fun testBufferFraming() = runBlocking {
        val transformer = InstrumentationResponseTransformer()

        val lines = javaClass.getResourceAsStream("/instrumentation/log_6.input").reader().readLines()

        val events = mutableListOf<TestEvent>()
        for (line in lines) {
            val part1 = line.substring(0, 7 * line.length / 8)
            val part2 = line.substring(7 * line.length / 8, line.length)
            val bytes1 = (part1).toByteArray(Const.DEFAULT_TRANSPORT_ENCODING)
            val bytes2 = (part2 + '\n').toByteArray(Const.DEFAULT_TRANSPORT_ENCODING)
            transformer.process(bytes1, 0, bytes1.size)?.let {
                events.addAll(it)
            }
            transformer.process(bytes2, 0, bytes2.size)?.let {
                events.addAll(it)
            }
        }
        transformer.transform()?.let { events.addAll(it) }

        assertThat(events.map { it.toString() }.reduce { acc, s -> acc + "\n" + s })
            .isEqualTo(javaClass.getResourceAsStream("/instrumentation/log_6.expected").reader().readText())
    }

    /**
     * \r\n
     */
    @Test
    fun testWindowsLineEnding() = runBlocking {
        val transformer = InstrumentationResponseTransformer()

        val lines = javaClass.getResourceAsStream("/instrumentation/log_3.input").reader().readLines()

        val events = mutableListOf<TestEvent>()
        for (line in lines) {
            val bytes = (line + "\r\n").toByteArray(Const.DEFAULT_TRANSPORT_ENCODING)
            transformer.process(bytes, 0, bytes.size)?.let {
                events.addAll(it)
            }
        }
        transformer.transform()?.let { events.addAll(it) }

        assertThat(events.map { it.toString() }.reduce { acc, s -> acc + "\n" + s })
            .isEqualTo(javaClass.getResourceAsStream("/instrumentation/log_3.expected").reader().readText())
    }

    @Test
    fun testSingleBlock() = runBlocking {
        val transformer = InstrumentationResponseTransformer()
        val lines = javaClass.getResourceAsStream("/instrumentation/log_7.input").reader().readText()

        val events = mutableListOf<TestEvent>()
        val bytes = (lines).toByteArray(Const.DEFAULT_TRANSPORT_ENCODING)
        transformer.process(bytes, 0, bytes.size)?.let {
            events.addAll(it)
        }
        transformer.transform()?.let { events.addAll(it) }

        assertThat(events.map { it.toString() }.reduce { acc, s -> acc + "\n" + s })
            .isEqualTo(javaClass.getResourceAsStream("/instrumentation/log_7.expected").reader().readText().trimEnd())
    }
}
