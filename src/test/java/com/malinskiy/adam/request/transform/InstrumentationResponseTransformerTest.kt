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
                event.add(TestRunStartedEvent(runName, testCount))
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

        val lines = javaClass.getResourceAsStream("/instrumentation/log_3.log").reader().readLines()
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

            val lines = javaClass.getResourceAsStream("/instrumentation/log_3.log").reader().readLines()

            val events = mutableListOf<TestEvent>()
            for (line in lines) {
                val bytes = (line + '\n').toByteArray(Const.DEFAULT_TRANSPORT_ENCODING)
                transformer.process(bytes, 0, bytes.size)
                transformer.transform()?.let {
                    events.add(it)
                }
            }

            val id = TestIdentifier("com.example.AbstractFailingTest", "testAlwaysFailing")
            assertThat(events)
                .containsExactly(
                    TestStarted(id),
                    TestFailed(
                        id, "java.lang.AssertionError\n" +
                                "\tat org.junit.Assert.fail(Assert.java:86)\n" +
                                "\tat org.junit.Assert.assertTrue(Assert.java:41)\n" +
                                "\tat org.junit.Assert.assertTrue(Assert.java:52)\n" +
                                "\tat com.example.AbstractFailingTest.testAlwaysFailing(AbstractFailingTest.kt:22)\n" +
                                "\tat java.lang.reflect.Method.invoke(Native Method)\n" +
                                "\tat org.junit.runners.model.FrameworkMethod\$1.runReflectiveCall(FrameworkMethod.java:50)\n" +
                                "\tat org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)\n" +
                                "\tat org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:47)\n" +
                                "\tat org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)\n" +
                                "\tat android.support.test.rule.ActivityTestRule\$ActivityStatement.evaluate(ActivityTestRule.java:433)\n" +
                                "\tat org.junit.rules.RunRules.evaluate(RunRules.java:20)\n" +
                                "\tat org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:325)\n" +
                                "\tat org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:78)\n" +
                                "\tat org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:57)\n" +
                                "\tat org.junit.runners.ParentRunner\$3.run(ParentRunner.java:290)\n" +
                                "\tat org.junit.runners.ParentRunner\$1.schedule(ParentRunner.java:71)\n" +
                                "\tat org.junit.runners.ParentRunner.runChildren(ParentRunner.java:288)\n" +
                                "\tat org.junit.runners.ParentRunner.access\$000(ParentRunner.java:58)\n" +
                                "\tat org.junit.runners.ParentRunner\$2.evaluate(ParentRunner.java:268)\n" +
                                "\tat org.junit.runners.ParentRunner.run(ParentRunner.java:363)\n" +
                                "\tat org.junit.runners.Suite.runChild(Suite.java:128)\n" +
                                "\tat org.junit.runners.Suite.runChild(Suite.java:27)\n" +
                                "\tat org.junit.runners.ParentRunner\$3.run(ParentRunner.java:290)\n" +
                                "\tat org.junit.runners.ParentRunner\$1.schedule(ParentRunner.java:71)\n" +
                                "\tat org.junit.runners.ParentRunner.runChildren(ParentRunner.java:288)\n" +
                                "\tat org.junit.runners.ParentRunner.access\$000(ParentRunner.java:58)\n" +
                                "\tat org.junit.runners.ParentRunner\$2.evaluate(ParentRunner.java:268)\n" +
                                "\tat org.junit.runners.ParentRunner.run(ParentRunner.java:363)\n" +
                                "\tat org.junit.runner.JUnitCore.run(JUnitCore.java:137)\n" +
                                "\tat org.junit.runner.JUnitCore.run(JUnitCore.java:115)\n" +
                                "\tat android.support.test.internal.runner.TestExecutor.execute(TestExecutor.java:58)\n" +
                                "\tat android.support.test.runner.AndroidJUnitRunner.onStart(AndroidJUnitRunner.java:375)\n" +
                                "\tat android.app.Instrumentation\$InstrumentationThread.run(Instrumentation.java:1932)\n"
                    ),
                    TestEnded(id, emptyMap()),
                    TestRunEnded(638L, emptyMap())
                )
        }
    }

}