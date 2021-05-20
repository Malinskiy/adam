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

package com.malinskiy.adam.request

sealed class Target {
    abstract fun serialize(): String
}

/**
 * When asking for information related to a device, 'host:' can also be
 * interpreted as 'any single device or emulator connected to/running on
 * the host'.
 */
object HostTarget : Target() {
    override fun serialize() = "host:"
}

/**
 * This is a special form of query, where the 'host-serial:<serial-number>:'
 * prefix can be used to indicate that the client is asking the ADB server
 * for information related to a specific device.
 */
class SerialTarget(private val serial: String) : Target() {
    override fun serialize() = "host-serial:$serial:"
}

/**
 * A variant of host-serial used to target the single USB device connected
 * to the host. This will fail if there is none or more than one.
 */
object UsbTarget : Target() {
    override fun serialize() = "host-usb:"
}

/**
 * A variant of host-serial used to target the single emulator instance
 * running on the host. This will fail if there is none or more than one.
 */
object LocalTarget : Target() {
    override fun serialize() = "host-local:"
}

object NonSpecifiedTarget : Target() {
    override fun serialize() = ""
}
