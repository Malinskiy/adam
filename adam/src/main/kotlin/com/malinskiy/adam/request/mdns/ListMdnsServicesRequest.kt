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

class ListMdnsServicesRequest : ComplexRequest<List<MdnsService>>(target = HostTarget) {
    override suspend fun readElement(socket: Socket): List<MdnsService> {

        return socket.readProtocolString().lines()
            .filterNot { it.isEmpty() }
            .map {
                val split = it.split(' ', '\t')
                MdnsService(
                    name = split[0].trim(),
                    serviceType = split[1].trim(),
                    url = split[2].trim()
                )
            }
    }

    override fun serialize() = createBaseRequest("mdns:services")
}
