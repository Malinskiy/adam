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

package com.malinskiy.adam.request.device

import com.malinskiy.adam.extension.readProtocolString
import com.malinskiy.adam.request.ComplexRequest
import com.malinskiy.adam.request.HostTarget
import com.malinskiy.adam.transport.Socket

class ListDevicesRequest : ComplexRequest<List<Device>>(target = HostTarget) {
    override fun serialize() = createBaseRequest("devices")

    override suspend fun readElement(socket: Socket): List<Device> {
        return socket.readProtocolString().lines()
            .filter { it.isNotEmpty() }
            .map {
                val line = it.trim()
                val split = line.split("\t")
                Device(
                    serial = split[0],
                    state = DeviceState.from(split[1])
                )
            }
    }
}

data class Device(val serial: String, val state: DeviceState)

enum class DeviceState {
    OFFLINE,
    BOOTLOADER,
    DEVICE,
    HOST,
    RECOVERY,
    RESCUE,
    SIDELOAD,
    UNAUTHORIZED,
    AUTHORIZING,
    CONNECTING,
    UNKNOWN;

    companion object {
        fun from(value: String) =
            when (value) {
                "offline" -> OFFLINE
                "bootloader" -> BOOTLOADER
                "device" -> DEVICE
                "host" -> HOST
                "recovery" -> RECOVERY
                "rescue" -> RESCUE
                "sideload" -> SIDELOAD
                "unauthorized" -> UNAUTHORIZED
                "authorizing" -> AUTHORIZING
                "connecting" -> CONNECTING
                else -> UNKNOWN
            }
    }
}
