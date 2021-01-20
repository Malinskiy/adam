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

import com.malinskiy.adam.extension.copyTo
import com.malinskiy.adam.extension.readStatus
import com.malinskiy.adam.request.ComplexRequest
import com.malinskiy.adam.transport.Socket
import com.malinskiy.adam.transport.withMaxFilePacketBuffer
import io.ktor.utils.io.*

/**
 * Executes the command and provides the channel as the input to the command. Does not return anything
 */
class ExecInRequest(private val cmd: String, private val channel: ByteReadChannel) : ComplexRequest<Unit>() {
    override suspend fun readElement(socket: Socket) {
        withMaxFilePacketBuffer {
            channel.copyTo(socket, this)
            //Have to poll
            socket.readStatus()
        }
    }

    override fun serialize() = createBaseRequest("exec:$cmd")
}
