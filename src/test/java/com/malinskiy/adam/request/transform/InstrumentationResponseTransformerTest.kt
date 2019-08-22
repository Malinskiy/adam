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
import assertk.assertions.isEqualTo
import com.android.ddmlib.receiver.InstrumentationResultParser
import com.android.ddmlib.testrunner.ITestRunListener
import com.malinskiy.adam.Const
import com.malinskiy.adam.request.testrunner.*
import kotlinx.coroutines.runBlocking
import org.junit.Test

class InstrumentationResponseTransformerTest {

    @Test
    fun testX() {
        val event = mutableListOf<TestEvent>()

        val parser = InstrumentationResultParser("com.example.test", object : ITestRunListener {
            override fun testRunStarted(runName: String, testCount: Int) {
                event.add(TestRunStartedEvent(testCount))
            }

            override fun testStarted(test: com.android.ddmlib.testrunner.TestIdentifier) {
                event.add(TestStarted(convert(test)))
            }

            override fun testFailed(test: com.android.ddmlib.testrunner.TestIdentifier, trace: String) {
                event.add(TestFailed(convert(test), trace))
            }

            override fun testAssumptionFailure(test: com.android.ddmlib.testrunner.TestIdentifier, trace: String) {
                event.add(TestAssumptionFailed(convert(test), trace))
            }

            override fun testIgnored(test: com.android.ddmlib.testrunner.TestIdentifier) {
                event.add(TestIgnored(convert(test)))
            }

            override fun testEnded(test: com.android.ddmlib.testrunner.TestIdentifier, testMetrics: MutableMap<String, String>) {
                event.add(TestEnded(convert(test), testMetrics))
            }

            override fun testRunFailed(errorMessage: String) {
                event.add(TestRunFailed(errorMessage))
            }

            override fun testRunStopped(elapsedTime: Long) {
                event.add(TestRunStopped(elapsedTime))
            }

            override fun testRunEnded(elapsedTime: Long, runMetrics: MutableMap<String, String>) {
                event.add(TestRunEnded(elapsedTime, runMetrics))
            }

            private fun convert(test: com.android.ddmlib.testrunner.TestIdentifier) = TestIdentifier(test.className, test.testName)
        })

        val lines = javaClass.getResourceAsStream("/instrumentation/log_1.input").reader().readLines()
        parser.processNewLines(lines.toTypedArray())

        parser.done()

        for (testEvent in event) {
            println(testEvent)
        }
    }

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
}