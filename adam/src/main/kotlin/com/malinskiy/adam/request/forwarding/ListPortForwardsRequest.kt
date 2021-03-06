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

import com.malinskiy.adam.extension.readProtocolString
import com.malinskiy.adam.request.ComplexRequest
import com.malinskiy.adam.request.SerialTarget
import com.malinskiy.adam.transport.Socket

class ListPortForwardsRequest(serial: String) : ComplexRequest<List<PortForwardingRule>>(target = SerialTarget(serial)) {
    override suspend fun readElement(socket: Socket): List<PortForwardingRule> {
        return socket.readProtocolString().lines().mapNotNull { line ->
            if (line.isNotEmpty()) {
                val split = line.split(" ")
                PortForwardingRule(
                    serial = split[0],
                    localSpec = LocalPortSpec.parse(split[1]),
                    remoteSpec = RemotePortSpec.parse(split[2])
                )
            } else {
                null
            }
        }
    }

    override fun serialize() = createBaseRequest("list-forward")
}

