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

package com.malinskiy.adam.request.forwarding

import com.malinskiy.adam.exception.RequestRejectedException
import com.malinskiy.adam.request.ComplexRequest
import com.malinskiy.adam.request.SerialTarget
import com.malinskiy.adam.transport.AndroidReadChannel
import com.malinskiy.adam.transport.AndroidWriteChannel

/**
 * Optionally returns a local TCP port that is occupied now if using LocalTcpPortSpec without any parameters
 */
class PortForwardRequest(
    private val local: LocalPortSpec,
    private val remote: RemotePortSpec,
    serial: String,
    private val mode: PortForwardingMode = PortForwardingMode.DEFAULT

) : ComplexRequest<Int?>(target = SerialTarget(serial)) {

    override fun serialize() =
        createBaseRequest("forward${mode.value}:${local.toSpec()};${remote.toSpec()}")

    override suspend fun readElement(readChannel: AndroidReadChannel, writeChannel: AndroidWriteChannel): Int? {
        val transportResponse = readChannel.read()
        if (!transportResponse.okay) {
            throw RequestRejectedException("Can't establish port forwarding: ${transportResponse.message ?: ""}")
        }

        return readChannel.readOptionalProtocolString()?.toIntOrNull()
    }
}

enum class PortForwardingMode(val value: String) {
    DEFAULT(""),
    NO_REBIND(":norebind")
}
