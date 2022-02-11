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
import com.malinskiy.adam.extension.readStatus
import com.malinskiy.adam.request.ComplexRequest
import com.malinskiy.adam.transport.Socket
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel

/**
 * Executes the command and provides the channel as the input to the command. Does not return anything
 */
class ExecInRequest(
    private val cmd: String,
    private val channel: ReceiveChannel<ByteArray>,
    private val sizeChannel: SendChannel<Int>,
    socketIdleTimeout: Long? = null
) :
    ComplexRequest<Unit>(socketIdleTimeout = socketIdleTimeout) {
    override suspend fun readElement(socket: Socket) {
        while (true) {
            if (!channel.isClosedForReceive) {
                //Should not request more if read channel is already closed
                sizeChannel.send(Const.MAX_FILE_PACKET_LENGTH)
            }
            val result = channel.tryReceive()
            when {
                result.isSuccess && result.getOrThrow().isNotEmpty() -> socket.writeFully(result.getOrThrow(), 0, result.getOrThrow().size)
                result.isClosed -> break
                result.isFailure -> continue
                else -> break
            }
        }
        //Have to poll
        socket.readStatus()
    }

    override fun serialize() = createBaseRequest("exec:$cmd")
}
