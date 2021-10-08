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

package com.malinskiy.adam.request.misc

import com.malinskiy.adam.Const
import com.malinskiy.adam.extension.compatClear
import com.malinskiy.adam.extension.compatFlip
import com.malinskiy.adam.extension.compatLimit
import com.malinskiy.adam.request.ComplexRequest
import com.malinskiy.adam.request.NonSpecifiedTarget
import com.malinskiy.adam.request.Target
import com.malinskiy.adam.transport.Socket
import com.malinskiy.adam.transport.withDefaultBuffer

/**
 * This request is quite tricky to use since the target of the request varies with the reconnection target
 *
 * If you don't specify anything in reconnectTarget then it's treated as `find the first available device` and reconnect
 * If you specify Device target then you have to provide the target either here or via serial during execution
 * If you use Offline then you have to use the host target only
 *
 * @param reconnectTarget default behaviour depends on the target serial that will be specified during execution
 */
class ReconnectRequest(
    private val reconnectTarget: ReconnectTarget? = null,
    target: Target = NonSpecifiedTarget
) : ComplexRequest<String>(target = target) {
    override suspend fun readElement(socket: Socket): String {
        withDefaultBuffer {
            compatLimit(4)
            socket.readFully(this)
            compatFlip()
            return if (array().copyOfRange(0, 4).contentEquals(done)) {
                "done"
            } else {
                //This is length of a response string
                val size = String(array(), 0, 4, Const.DEFAULT_TRANSPORT_ENCODING).toInt(radix = 16)
                compatClear()
                compatLimit(size)
                socket.readFully(this)
                compatFlip()
                String(array(), 0, size, Const.DEFAULT_TRANSPORT_ENCODING)
            }
        }
    }

    override fun serialize() = when (reconnectTarget) {
        null -> createBaseRequest("reconnect")
        Device -> createBaseRequest("reconnect")
        Offline -> createBaseRequest("reconnect-offline")
    }

    companion object {
        /**
         * For some reason this done is lowercase and doesn't use the DONE message as everything else
         * see daemon/services.cpp#reconnect_service
         */
        val done = byteArrayOf(
            'd'.code.toByte(),
            'o'.code.toByte(),
            'n'.code.toByte(),
            'e'.code.toByte(),
        )
    }
}

sealed class ReconnectTarget
object Device : ReconnectTarget()

/**
 * Only supports host target
 */
object Offline : ReconnectTarget()
