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

package com.malinskiy.adam.request.testrunner.model

enum class Status(val value: Int) {
    SUCCESS(0),
    START(1),
    IN_PROGRESS(2),

    /**
     * JUnit3 runner code, treated as FAILURE
     */
    ERROR(-1),
    FAILURE(-2),
    IGNORED(-3),
    ASSUMPTION_FAILURE(-4),
    UNKNOWN(6666);

    fun isTerminal() = value < 0

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
