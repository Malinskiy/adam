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
import assertk.assertions.isEqualTo
import com.malinskiy.adam.request.testrunner.TestEvent
import com.malinskiy.adam.request.transform.ProtoInstrumentationResponseTransformer
import kotlinx.coroutines.runBlocking
import org.junit.Test

class ProtoInstrumentationResponseTransformerTest {
    @Test
    fun testSingleFailure() {
        runBlocking {
            val transformer = ProtoInstrumentationResponseTransformer()

            val protoBytes = javaClass.getResourceAsStream("/instrumentation/proto_1.input").readBytes()

            val events = mutableListOf<TestEvent>()

            transformer.process(protoBytes, 0, 175)?.let { events.addAll(it) }
            transformer.process(protoBytes, 175, 11776)?.let { events.addAll(it) }
            transformer.process(protoBytes, 175 + 11776, 8347)?.let { events.addAll(it) }
            transformer.process(protoBytes, 175 + 11776 + 8347, 2466)?.let { events.addAll(it) }
            transformer.transform()?.let { events.addAll(it) }

            assertThat(events.map { it.toString() }.reduce { acc, s -> acc + "\n" + s })
                .isEqualTo(javaClass.getResourceAsStream("/instrumentation/proto_log_1.expected").reader().readText())
        }
    }
}
