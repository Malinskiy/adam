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
import com.malinskiy.adam.AndroidDebugBridgeClient
import com.malinskiy.adam.request.Feature
import com.malinskiy.adam.server.junit4.AdbServerRule
import io.ktor.utils.io.discard
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

class TestRunnerRequestTest {
    @get:Rule
    val server = AdbServerRule()
    val client: AndroidDebugBridgeClient
        get() = server.client

    @Test
    fun testSerialize() {
        runBlocking {
            val request = TestRunnerRequest(
                testPackage = "com.example.test",
                noWindowAnimations = true,
                instrumentOptions = InstrumentOptions(),
                abi = "x86",
                noHiddenApiChecks = true,
                outputLogPath = "/sdcard/log",
                profilingOutputPath = "/sdcard/profiling",
                runnerClass = "com.example.test.AndroidTestRunner",
                userId = 1000,
                supportedFeatures = emptyList(),
                coroutineScope = this
            )

            assertThat(request.cmd)
                .isEqualTo(
                    "am instrument -w -r --no-hidden-api-checks --no-window-animation --user 1000 --abi x86 " +
                            "-p /sdcard/profiling -f /sdcard/log com.example.test/com.example.test.AndroidTestRunner"
                )
        }
    }

    @Test
    fun testDefaultSerialize() {
        runBlocking {
            val request = TestRunnerRequest(
                testPackage = "com.example.test",
                instrumentOptions = InstrumentOptions(),
                supportedFeatures = emptyList(),
                coroutineScope = this
            )

            assertThat(request.cmd)
                .isEqualTo(
                    "am instrument -w -r com.example.test/android.support.test.runner.AndroidJUnitRunner"
                )
        }
    }

    @Test
    fun testV1() {
        runBlocking {
            launch {
                server.session {
                    expectCmd { "host:transport:serial" }.accept()
                    expectShell { "am instrument -w -r com.example.test/android.support.test.runner.AndroidJUnitRunner" }
                        .accept()
                        .respond(
                            """
                            INSTRUMENTATION_STATUS: class=com.example.MainActivityAllureTest
                            INSTRUMENTATION_STATUS: current=1
                            INSTRUMENTATION_STATUS: id=AndroidJUnitRunner
                            INSTRUMENTATION_STATUS: numtests=1
                            INSTRUMENTATION_STATUS: stream=
                            com.example.MainActivityAllureTest:
                            INSTRUMENTATION_STATUS: test=testText
                            INSTRUMENTATION_STATUS_CODE: 1
                            INSTRUMENTATION_STATUS: class=com.example.MainActivityAllureTest
                            INSTRUMENTATION_STATUS: current=1
                            INSTRUMENTATION_STATUS: id=AndroidJUnitRunner
                            INSTRUMENTATION_STATUS: numtests=1
                            INSTRUMENTATION_STATUS: stream=.
                            INSTRUMENTATION_STATUS: test=testText
                            INSTRUMENTATION_STATUS_CODE: 0
                            INSTRUMENTATION_RESULT: stream=

                            Time: 0.915

                            OK (1 test)


                            INSTRUMENTATION_CODE: -1
                        """.trimIndent()
                        )
                }

                val channel = client.execute(
                    TestRunnerRequest(
                        testPackage = "com.example.test",
                        instrumentOptions = InstrumentOptions(),
                        supportedFeatures = emptyList(),
                        coroutineScope = this
                    ),
                    serial = "serial",
                )
                val events = mutableListOf<TestEvent>()
                while (!channel.isClosedForReceive) {
                    val chunk = channel.receiveCatching().getOrNull() ?: break
                    events.addAll(chunk)
                }

                assertThat(events).containsExactly(
                    TestRunStartedEvent(testCount = 1),
                    TestStarted(id = TestIdentifier(className = "com.example.MainActivityAllureTest", testName = "testText")),
                    TestEnded(
                        id = TestIdentifier(className = "com.example.MainActivityAllureTest", testName = "testText"),
                        metrics = emptyMap()
                    ),
                    TestRunEnded(elapsedTimeMillis = 0, metrics = emptyMap())
                )
            }.join()
        }
    }

    @Test
    fun testV2() {
        runBlocking {
            launch {
                server.session {
                    expectCmd { "host:transport:serial" }.accept()
                    expectShellV2 { "am instrument -w -r com.example.test/android.support.test.runner.AndroidJUnitRunner" }
                        .accept()
                        .respondStdout(
                            """
                            INSTRUMENTATION_STATUS: class=com.example.MainActivityAllureTest
                            INSTRUMENTATION_STATUS: current=1
                            INSTRUMENTATION_STATUS: id=AndroidJUnitRunner
                            INSTRUMENTATION_STATUS: numtests=1
                            INSTRUMENTATION_STATUS: stream=
                            com.example.MainActivityAllureTest:
                            INSTRUMENTATION_STATUS: test=testText
                            INSTRUMENTATION_STATUS_CODE: 1
                            INSTRUMENTATION_STATUS: class=com.example.MainActivityAllureTest
                            INSTRUMENTATION_STATUS: current=1
                            INSTRUMENTATION_STATUS: id=AndroidJUnitRunner
                            INSTRUMENTATION_STATUS: numtests=1
                            INSTRUMENTATION_STATUS: stream=.
                            INSTRUMENTATION_STATUS: test=testText
                            INSTRUMENTATION_STATUS_CODE: 0
                            INSTRUMENTATION_RESULT: stream=

                            Time: 0.915

                            OK (1 test)


                            INSTRUMENTATION_CODE: -1
                        """.trimIndent()
                        )
                        .respondExit(0)
                }

                val channel = client.execute(
                    TestRunnerRequest(
                        testPackage = "com.example.test",
                        instrumentOptions = InstrumentOptions(),
                        supportedFeatures = listOf(Feature.SHELL_V2),
                        coroutineScope = this
                    ),
                    serial = "serial",
                )
                val events = mutableListOf<TestEvent>()
                while (!channel.isClosedForReceive) {
                    val chunk = channel.receiveCatching().getOrNull() ?: break
                    events.addAll(chunk)
                }

                assertThat(events).containsExactly(
                    TestRunStartedEvent(testCount = 1),
                    TestStarted(id = TestIdentifier(className = "com.example.MainActivityAllureTest", testName = "testText")),
                    TestEnded(
                        id = TestIdentifier(className = "com.example.MainActivityAllureTest", testName = "testText"),
                        metrics = emptyMap()
                    ),
                    TestRunEnded(elapsedTimeMillis = 0, metrics = emptyMap())
                )
            }.join()
        }
    }

    @Test
    fun testV2IgnoresStderr() {
        runBlocking {
            launch {
                server.session {
                    expectCmd { "host:transport:serial" }.accept()
                    expectShellV2 { "am instrument -w -r com.example.test/android.support.test.runner.AndroidJUnitRunner" }
                        .accept()
                        .respondStdout(
                            """
                            INSTRUMENTATION_STATUS: class=com.example.MainActivityAllureTest
                            INSTRUMENTATION_STATUS: current=1
                            INSTRUMENTATION_STATUS: id=AndroidJUnitRunner
                            INSTRUMENTATION_STATUS: numtests=1
                            INSTRUMENTATION_STATUS: stream=
                            com.example.MainActivityAllureTest:
                            INSTRUMENTATION_STATUS: test=testText                          
                        """.trimIndent()
                        )
                        .respondStderr(
                            """
                            s_glBindAttribLocation: bind attrib 0 name position
                            s_glBindAttribLocation: bind attrib 1 name localCoord
                        """.trimIndent()
                        )
                        .respondStdout(
                            """
                                
                            INSTRUMENTATION_STATUS_CODE: 1
                            INSTRUMENTATION_STATUS: class=com.example.MainActivityAllureTest
                            INSTRUMENTATION_STATUS: current=1
                            INSTRUMENTATION_STATUS: id=AndroidJUnitRunner
                            INSTRUMENTATION_STATUS: numtests=1
                            INSTRUMENTATION_STATUS: stream=.
                            INSTRUMENTATION_STATUS: test=testText
                            INSTRUMENTATION_STATUS_CODE: 0
                            INSTRUMENTATION_RESULT: stream=

                            Time: 0.915

                            OK (1 test)


                            INSTRUMENTATION_CODE: -1
                        """.trimIndent()
                        )
                        .respondExit(0)
                }

                val channel = client.execute(
                    TestRunnerRequest(
                        testPackage = "com.example.test",
                        instrumentOptions = InstrumentOptions(),
                        supportedFeatures = listOf(Feature.SHELL_V2),
                        coroutineScope = this
                    ),
                    serial = "serial",
                )
                val events = mutableListOf<TestEvent>()
                for (chunk in channel) {
                    events.addAll(chunk)
                }

                assertThat(events).containsExactly(
                    TestRunStartedEvent(testCount = 1),
                    TestStarted(id = TestIdentifier(className = "com.example.MainActivityAllureTest", testName = "testText")),
                    TestEnded(
                        id = TestIdentifier(className = "com.example.MainActivityAllureTest", testName = "testText"),
                        metrics = emptyMap()
                    ),
                    TestRunEnded(elapsedTimeMillis = 0, metrics = emptyMap())
                )
            }.join()
        }
    }

    @Ignore
    @Test
    fun testReturnsContentOnFailure() {
        runBlocking {
            launch {
                server.session {
                    expectCmd { "host:transport:serial" }.accept()
                    expectShell { "am instrument -w -r com.example.test/android.support.test.runner.AndroidJUnitRunner" }
                        .accept()
                        .respond("something-something")
                }

                val channel = client.execute(
                    TestRunnerRequest(
                        testPackage = "com.example.test",
                        instrumentOptions = InstrumentOptions(),
                        supportedFeatures = emptyList(),
                        coroutineScope = this
                    ),
                    serial = "serial",
                )
                val events = mutableListOf<TestEvent>()
                while (!channel.isClosedForReceive) {
                    val chunk = channel.receiveCatching().getOrNull() ?: break
                    events.addAll(chunk)
                }

                assertThat(events).containsOnly(TestRunFailed("No test results\nsomething-something"))
            }.join()
        }
    }

    @Ignore
    @Test
    fun testChannelIsEmpty() {
        runBlocking {
            launch {
                server.session {
                    expectCmd { "host:transport:serial" }.accept()
                    input.discard()
                }

                val request = TestRunnerRequest(
                    testPackage = "com.example.test",
                    instrumentOptions = InstrumentOptions(),
                    supportedFeatures = emptyList(),
                    coroutineScope = this
                )
                val execute = client.execute(request, "serial")

                var events = mutableListOf<TestEvent>()
                while (!execute.isClosedForReceive) {
                    events.addAll(execute.receiveCatching().getOrNull() ?: break)
                }

                assertThat(events).isEqualTo(1.0)
            }.join()

//            assertThat(tempFile.readBytes()).isEqualTo(fixture.readBytes())
        }
    }
}
