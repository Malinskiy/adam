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

package com.malinskiy.adam.server.stub.dsl

class Expectation {
    private val handlers = mutableMapOf<String, DeviceExpectation>()
    private var otherHandlers = mutableMapOf<String, suspend Session.() -> Unit>()

    fun serial(serialNo: String, block: DeviceExpectation.() -> Unit) {
        val deviceExpectation = handlers[serialNo] ?: DeviceExpectation(serialNo)
        handlers[serialNo] = deviceExpectation
        block(deviceExpectation)
    }

    fun other(transportString: String, block: suspend Session.() -> Unit) {
        assert(!otherHandlers.contains(transportString)) { "Handler for $transportString already exists" }
        otherHandlers[transportString] = block
    }

    suspend fun select(session: Session): Boolean {
        val transportCmd = session.input.receiveCommand()
        return if (transportCmd.startsWith("host:transport:")) {
            val serial = transportCmd.substringAfter("host:transport:")
            handlers[serial]?.let { it.handle(session); true } ?: false
        } else {
            otherHandlers[transportCmd]?.invoke(session) ?: return false
            return true
        }
    }
}

