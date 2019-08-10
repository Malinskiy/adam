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

sealed class LocalPortSpec {
    abstract fun toSpec(): String

    companion object {
        fun parse(value: String): LocalPortSpec {
            val split = value.split(':')
            val type = split[0]
            return when (type) {
                "tcp" -> LocalTcpPortSpec(split[1].toInt())
                "local" -> LocalUnixSocketPortSpec(split[1])
                else -> throw UnsupportedForwardingSpecException(type)
            }
        }
    }
}

class LocalTcpPortSpec(val port: Int) : LocalPortSpec() {
    override fun toSpec() = "tcp:$port"
}

class LocalUnixSocketPortSpec(val path: String) : LocalPortSpec() {
    override fun toSpec() = "local:$path"
}