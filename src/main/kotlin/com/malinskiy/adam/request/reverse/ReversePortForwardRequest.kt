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

package com.malinskiy.adam.request.reverse

import com.malinskiy.adam.exception.RequestRejectedException
import com.malinskiy.adam.request.ComplexRequest
import com.malinskiy.adam.request.NonSpecifiedTarget
import com.malinskiy.adam.request.forwarding.LocalPortSpec
import com.malinskiy.adam.request.forwarding.PortForwardingMode
import com.malinskiy.adam.request.forwarding.RemotePortSpec
import com.malinskiy.adam.transport.AndroidReadChannel
import com.malinskiy.adam.transport.AndroidWriteChannel

/**
 * Doesn't work with SerialTarget, have to use the serial as a parameter for the execute method
 */
class ReversePortForwardRequest(
    private val local: RemotePortSpec,
    private val remote: LocalPortSpec,
    private val mode: PortForwardingMode = PortForwardingMode.DEFAULT

) : ComplexRequest<Int?>(target = NonSpecifiedTarget) {

    override fun serialize() =
        createBaseRequest("reverse:forward${mode.value}:${local.toSpec()};${remote.toSpec()}")

    override suspend fun readElement(readChannel: AndroidReadChannel, writeChannel: AndroidWriteChannel): Int? {
        val transportResponse = readChannel.read()
        if (!transportResponse.okay) {
            throw RequestRejectedException("Can't establish port forwarding: ${transportResponse.message ?: ""}")
        }

        return readChannel.readOptionalProtocolString()?.toIntOrNull()
    }
}
