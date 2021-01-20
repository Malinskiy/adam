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

package com.malinskiy.adam.request.device

import com.malinskiy.adam.extension.readProtocolString
import com.malinskiy.adam.request.AsyncChannelRequest
import com.malinskiy.adam.request.HostTarget
import com.malinskiy.adam.transport.Socket
import kotlinx.coroutines.channels.SendChannel

class AsyncDeviceMonitorRequest : AsyncChannelRequest<List<Device>, Unit>(target = HostTarget) {
    override suspend fun readElement(socket: Socket, sendChannel: SendChannel<List<Device>>): Boolean {
        sendChannel.send(socket.readProtocolString().lines()
                             .filter { it.isNotEmpty() }
                             .map {
                                 val line = it.trim()
                                 val split = line.split("\t")
                                 Device(
                                     serial = split[0],
                                     state = DeviceState.from(split[1])
                                 )
                             }
        )
        return false
    }

    override fun serialize() = createBaseRequest("track-devices")
    override suspend fun writeElement(element: Unit, socket: Socket) = Unit
}
