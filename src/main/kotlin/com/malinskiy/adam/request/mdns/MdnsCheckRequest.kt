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

package com.malinskiy.adam.request.mdns

import com.malinskiy.adam.extension.readProtocolString
import com.malinskiy.adam.request.ComplexRequest
import com.malinskiy.adam.request.HostTarget
import com.malinskiy.adam.transport.Socket

/**
 * check if mdns discovery is available
 */
class MdnsCheckRequest : ComplexRequest<MdnsStatus>(target = HostTarget) {
    override suspend fun readElement(socket: Socket): MdnsStatus {
        val string = socket.readProtocolString()
        return if (string.contains("mdns daemon unavailable")) {
            MdnsStatus(false)
        } else {
            val version = string.substringAfterLast('[').substringBeforeLast(']')
            MdnsStatus(available = true, version = version)
        }
    }

    override fun serialize() = createBaseRequest("mdns:check")
}
