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

package com.malinskiy.adam.request.transform

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import com.malinskiy.adam.Const
import com.malinskiy.adam.request.testrunner.TestEvent
import com.malinskiy.adam.request.testrunner.TestIdentifier
import com.malinskiy.adam.request.testrunner.TestRunFailed
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
                transformer.process(bytes, 0, bytes.size)
                transformer.transform()?.let {
                    events.addAll(it)
                }
            }
            transformer.close()?.let { events.addAll(it) }

            val id = TestIdentifier("com.example.AbstractFailingTest", "testAlwaysFailing")
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
                transformer.process(bytes, 0, bytes.size)
                transformer.transform()?.let {
                    events.addAll(it)
                }
            }
            transformer.close()?.let { events.addAll(it) }

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
                transformer.process(bytes, 0, bytes.size)
                transformer.transform()?.let {
                    events.addAll(it)
                }
            }
            transformer.close()?.let { events.addAll(it) }

            assertThat(events.map { it.toString() }.reduce { acc, s -> acc + "\n" + s })
                .isEqualTo(javaClass.getResourceAsStream("/instrumentation/log_1.expected").reader().readText())
        }
    }

    @Test
    fun testNoResultsReported() {
        runBlocking {
            val transformer = InstrumentationResponseTransformer()
            val list = transformer.close()
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
                transformer.process(bytes, 0, bytes.size)
                transformer.transform()?.let {
                    events.addAll(it)
                }
            }
            transformer.close()?.let { events.addAll(it) }

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
                transformer.process(bytes, 0, bytes.size)
                transformer.transform()?.let {
                    events.addAll(it)
                }
            }
            transformer.close()?.let { events.addAll(it) }

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
            transformer.process(bytes, 0, bytes.size)
            transformer.transform()?.let {
                events.addAll(it)
            }
        }
        transformer.close()?.let { events.addAll(it) }

        assertThat(events.map { it.toString() }.reduce { acc, s -> acc + "\n" + s })
            .isEqualTo(javaClass.getResourceAsStream("/instrumentation/log_6.expected").reader().readText())
    }
}

