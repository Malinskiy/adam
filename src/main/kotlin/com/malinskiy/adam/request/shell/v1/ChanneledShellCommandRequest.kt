/*
 * Copyright (C) 2020 Anton Malinskiy
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

package com.malinskiy.adam.request.shell.v1

import com.malinskiy.adam.Const
import com.malinskiy.adam.request.AsyncChannelRequest
import com.malinskiy.adam.request.NonSpecifiedTarget
import com.malinskiy.adam.request.Target
import com.malinskiy.adam.transport.Socket

open class ChanneledShellCommandRequest(
    val cmd: String,
    target: Target = NonSpecifiedTarget
) : AsyncChannelRequest<String, Unit>(target = target) {

    val data = ByteArray(Const.MAX_PACKET_LENGTH)

    override suspend fun readElement(socket: Socket): String? {
        val count = socket.readAvailable(data, 0, Const.MAX_PACKET_LENGTH)
        return when {
            count > 0 -> String(data, 0, count, Const.DEFAULT_TRANSPORT_ENCODING)
            else -> return null
        }
    }

    override fun serialize() = createBaseRequest("shell:$cmd")
    override suspend fun writeElement(element: Unit, socket: Socket) = Unit
}
