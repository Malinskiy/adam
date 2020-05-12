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

import com.malinskiy.adam.exception.UnsupportedForwardingSpecException

sealed class RemotePortSpec {
    abstract fun toSpec(): String

    companion object {
        fun parse(value: String): RemotePortSpec {
            val split = value.split(':')
            val type = split[0]
            return when(type) {
                "tcp" -> RemoteTcpPortSpec(split[1].toInt())
                "localabstract" -> RemoteAbstractPortSpec(split[1])
                "localreserved" -> RemoteReservedPortSpec(split[1])
                "localfilesystem" -> RemoteFilesystemPortSpec(split[1])
                "dev" -> RemoteDevPortSpec(split[1])
                "jdwp" -> JDWPPortSpec(split[1].toInt())
                else -> throw UnsupportedForwardingSpecException(type)
            }
        }
    }
}

data class RemoteTcpPortSpec(val port: Int) : RemotePortSpec() {
    override fun toSpec() = "tcp:$port"
}

data class RemoteAbstractPortSpec(val unixDomainSocketName: String) : RemotePortSpec() {
    override fun toSpec() = "localabstract:$unixDomainSocketName"
}

data class RemoteReservedPortSpec(val unixDomainSocketName: String) : RemotePortSpec() {
    override fun toSpec() = "localreserved:$unixDomainSocketName"
}

data class RemoteFilesystemPortSpec(val unixDomainSocketName: String) : RemotePortSpec() {
    override fun toSpec() = "localfilesystem:$unixDomainSocketName"
}

data class RemoteDevPortSpec(val charDeviceName: String) : RemotePortSpec() {
    override fun toSpec() = "dev:$charDeviceName"
}

data class JDWPPortSpec(val processId: Int) : RemotePortSpec() {
    override fun toSpec() = "jdwp:$processId"
}