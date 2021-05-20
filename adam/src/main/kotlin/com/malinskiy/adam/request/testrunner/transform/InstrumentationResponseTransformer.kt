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

package com.malinskiy.adam.request.transform

import com.malinskiy.adam.Const
import com.malinskiy.adam.request.testrunner.TestAssumptionFailed
import com.malinskiy.adam.request.testrunner.TestEnded
import com.malinskiy.adam.request.testrunner.TestEvent
import com.malinskiy.adam.request.testrunner.TestFailed
import com.malinskiy.adam.request.testrunner.TestIdentifier
import com.malinskiy.adam.request.testrunner.TestIgnored
import com.malinskiy.adam.request.testrunner.TestRunEnded
import com.malinskiy.adam.request.testrunner.TestRunFailed
import com.malinskiy.adam.request.testrunner.TestRunStartedEvent
import com.malinskiy.adam.request.testrunner.TestStarted
import com.malinskiy.adam.request.testrunner.model.Status
import com.malinskiy.adam.request.testrunner.model.TokenType

class InstrumentationResponseTransformer : ProgressiveResponseTransformer<List<TestEvent>?> {
    var buffer = StringBuffer()

    private var startReported = false
    private var finishReported = false
    private var finished = false
    private var testsExpected = 0
    private var testsExecuted = 0

    override suspend fun process(bytes: ByteArray, offset: Int, limit: Int): List<TestEvent>? {
        buffer.append(String(bytes, offset, limit, Const.DEFAULT_TRANSPORT_ENCODING))

        var result: MutableList<TestEvent>? = null
        while (true) {
            val atom = findAtom() ?: break
            parse(atom)?.let { events ->
                val localResult = result ?: mutableListOf()
                localResult.addAll(events)
                result = localResult
            }
        }

        return result
    }

    private fun findAtom(): List<String>? {
        val tokenPosition = buffer.indexOfAny(
            listOf(
                TokenType.INSTRUMENTATION_STATUS_CODE.name,
                TokenType.INSTRUMENTATION_CODE.name
            )
        )

        if (tokenPosition == -1) {
            return null
        }

        val nextLineBreak = buffer.indexOfAny(charArrayOf('\n', '\r'), startIndex = tokenPosition)
        if (nextLineBreak == -1) {
            return null
        }

        val atom = buffer.substring(0, nextLineBreak).lines()
        buffer = buffer.delete(0, nextLineBreak + 1)
        return atom
    }

    override fun transform(): List<TestEvent>? {
        if (finishReported) return null

        return if (!startReported) {
            buffer.insert(0, "No test results\n")
            listOf(TestRunFailed(buffer.trim().toString()))
        } else if (testsExpected > testsExecuted) {
            listOf(TestRunFailed("Test run failed to complete. Expected $testsExpected tests, executed $testsExecuted"))
        } else {
            val events = mutableListOf<TestEvent>()
            if (!startReported) {
                events.add(TestRunStartedEvent(0))
            }

            events.add(TestRunEnded(0, emptyMap()))
            finishReported = true
            events
        }
    }

    private fun parse(atom: List<String>): List<TestEvent>? {
        val last = atom.last()
        return when {
            last.startsWith(TokenType.INSTRUMENTATION_STATUS_CODE.name) -> {
                parseStatusCode(last, atom.subList(0, atom.size - 1))
            }
            last.startsWith(TokenType.INSTRUMENTATION_CODE.name) -> {
                finished = true
                parseInstrumentationCode(last, atom)
            }
            last.startsWith(TokenType.INSTRUMENTATION_FAILED.name) -> {
                finished = true
                null
            }
            else -> null
        }
    }

    /**
     * 1 - Test running
     * 0 - Test passed
     * -2 - assertion failure
     * -1 - other exceptions
     */
    private fun parseStatusCode(last: String, atom: List<String>): List<TestEvent>? {
        val value = last.substring(TokenType.INSTRUMENTATION_STATUS_CODE.name.length + 1).trim()
        val parameters: Map<String, String> = atom.toMap()

        val events = mutableListOf<TestEvent>()
        /**
         * Send [TestRunStartedEvent] if not done yet
         */
        if (!startReported) {
            val tests = parameters["numtests"]?.toInt()
            tests?.let {
                events.add(TestRunStartedEvent(it))
                testsExpected = tests
            }
            startReported = true
        }

        val metrics = emptyMap<String, String>()

        when (Status.valueOf(value.toIntOrNull())) {
            Status.SUCCESS -> {
                val className = parameters["class"]
                val testName = parameters["test"]
                if (className != null && testName != null) {
                    events.add(TestEnded(TestIdentifier(className, testName), metrics))
                }
                testsExecuted += 1
            }
            Status.START -> {
                val className = parameters["class"]
                val testName = parameters["test"]
                if (className != null && testName != null) {
                    events.add(TestStarted(TestIdentifier(className, testName)))
                }
            }
            Status.IN_PROGRESS -> Unit
            Status.ERROR, Status.FAILURE -> {
                val className = parameters["class"]
                val testName = parameters["test"]
                val stack = parameters["stack"]
                if (className != null && testName != null && stack != null) {
                    val id = TestIdentifier(className, testName)
                    events.add(TestFailed(id, stack))
                    events.add(TestEnded(id, metrics))
                }
                testsExecuted += 1
            }
            Status.IGNORED -> {
                val className = parameters["class"]
                val testName = parameters["test"]
                if (className != null && testName != null) {
                    val id = TestIdentifier(className, testName)
                    events.add(TestStarted(id))
                    events.add(TestIgnored(id))
                    events.add(TestEnded(id, metrics))
                }
                testsExecuted += 1
            }
            Status.ASSUMPTION_FAILURE -> {
                val className = parameters["class"]
                val testName = parameters["test"]
                val stack = parameters["stack"]
                if (className != null && testName != null && stack != null) {
                    val id = TestIdentifier(className, testName)
                    events.add(TestAssumptionFailed(id, stack))
                    events.add(TestEnded(id, metrics))
                }
                testsExecuted += 1
            }
            Status.UNKNOWN -> TODO()
        }

        return if (events.isNotEmpty()) {
            events
        } else {
            null
        }
    }

    /**
     * Session Result Code:
     * -1: Success
     * other: Failure
     */
    private fun parseInstrumentationCode(
        last: String,
        atom: List<String>
    ): List<TestEvent>? {
        val value = last.substring(TokenType.INSTRUMENTATION_CODE.name.length + 1).trim()
        val code = value.toIntOrNull()
        return when (Status.valueOf(code)) {
            Status.ERROR -> {
                var time = 0L
                val metrics = mutableMapOf<String, String>()

                atom.forEach { line ->
                    when {
                        line.startsWith("Time: ") -> {
                            time = line.substring(6).toDoubleOrNull()?.times(1000)?.toLong() ?: 0L
                        }
                    }
                }
                finishReported = true
                listOf(TestRunEnded(time, metrics))
            }
            else -> {
                finishReported = true
                listOf(TestRunFailed("Unexpected INSTRUMENTATION_CODE: $code"))
            }
        }
    }
}

private fun List<String>.toMap(): Map<String, String> {
    return this.filter { it.isNotEmpty() }.joinToString(separator = "\n").split("INSTRUMENTATION_STATUS: ").mapNotNull {
        /**
         * Generally, the stacktrace field will have only a single = sign.
         * But as observed on Sony Xperia D5833, it can contain multiple `=` signs (because stacktrace value is equal to the stream)
         */
        val trimmed = it.trim()
        val delimiterIndex = trimmed.indexOf('=')

        if (delimiterIndex + 1 >= trimmed.length) return@mapNotNull null

        Pair(trimmed.substring(0, delimiterIndex), trimmed.substring(delimiterIndex + 1, trimmed.length))
    }.toMap()
}

