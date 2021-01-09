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

package com.malinskiy.adam.request.testrunner

sealed class TestEvent

data class TestRunStartedEvent(val testCount: Int) : TestEvent()
data class TestStarted(val id: TestIdentifier) : TestEvent()
data class TestFailed(val id: TestIdentifier, val stackTrace: String) : TestEvent()
data class TestAssumptionFailed(val id: TestIdentifier, val stackTrace: String) : TestEvent()
data class TestIgnored(val id: TestIdentifier) : TestEvent()
data class TestEnded(val id: TestIdentifier, val metrics: Map<String, String>) : TestEvent()
data class TestRunFailed(val error: String) : TestEvent()
data class TestRunStopped(val elapsedTimeMillis: Long) : TestEvent()
data class TestRunEnded(val elapsedTimeMillis: Long, val metrics: Map<String, String>) : TestEvent()
data class TestLogcat(val id: TestIdentifier, val log: String)
