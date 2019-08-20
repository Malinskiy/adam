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

import com.malinskiy.adam.Const
import com.malinskiy.adam.request.testrunner.TestEvent
import com.malinskiy.adam.request.testrunner.TestRunEnded

class InstrumentationResponseTransformer : ResponseTransformer<TestEvent?> {
    val buffer = StringBuffer()

    override suspend fun process(bytes: ByteArray, offset: Int, limit: Int) {
        buffer.append(String(bytes, offset, limit, Const.DEFAULT_TRANSPORT_ENCODING))
    }

    override fun transform(): TestEvent? {
        val lines = buffer.lines()

        val tokenPosition = lines.indexOfFirst {
//            it.startsWith(TokenType.INSTRUMENTATION_STATUS_CODE.name) ||
//                    it.startsWith(TokenType.INSTRUMENTATION_RESULT.name) ||
                    it.startsWith(TokenType.INSTRUMENTATION_CODE.name)
        }

        if (tokenPosition == -1) {
            return null
        }

        val atom = lines.subList(0, tokenPosition + 1)
        return parse(atom)
    }

    private fun parse(atom: List<String>): TestEvent? {
        val last = atom.last()
        return when {
            last.startsWith(TokenType.INSTRUMENTATION_STATUS_CODE.name) -> {
                null
            }
            last.startsWith(TokenType.INSTRUMENTATION_RESULT.name) -> {
                null
            }
            last.startsWith(TokenType.INSTRUMENTATION_CODE.name) -> {
                parseInstrumentationCode(last, atom)
            }
            else -> null
        }
    }

    private fun parseInstrumentationCode(
        last: String,
        atom: List<String>
    ): TestRunEnded? {
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
                TestRunEnded(time, metrics)
            }
            else -> null
    //                    Status.SUCCESS -> {
    //                        TestRunEnded()
    //                    }
        }
    }
}

enum class TokenType {
    INSTRUMENTATION_STATUS,
    INSTRUMENTATION_STATUS_CODE,
    INSTRUMENTATION_RESULT,
    INSTRUMENTATION_CODE
}

enum class Status(val value: Int) {
    SUCCESS(0),
    START(1),
    IN_PROGRESS(2),
    ERROR(-1),
    FAILURE(-2),
    IGNORED(-3),
    ASSUMPTION_FAILURE(-4),
    UNKNOWN(6666);

    companion object {
        fun valueOf(value: Int?): Status {
            return when (value) {
                0 -> SUCCESS
                1 -> START
                2 -> IN_PROGRESS
                -1 -> ERROR
                -2 -> FAILURE
                -3 -> IGNORED
                -4 -> ASSUMPTION_FAILURE
                else -> UNKNOWN
            }
        }
    }
}