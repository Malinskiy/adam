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

package com.malinskiy.adam.request.sync.v1

import com.malinskiy.adam.Const
import com.malinskiy.adam.extension.compatFlip
import com.malinskiy.adam.request.sync.base.BasePushFileRequest
import com.malinskiy.adam.transport.Socket
import com.malinskiy.adam.transport.withDefaultBuffer
import kotlinx.coroutines.Dispatchers
import java.io.File
import kotlin.coroutines.CoroutineContext

class PushFileRequest(
    local: File,
    remotePath: String,
    mode: String = "0777",
    coroutineContext: CoroutineContext = Dispatchers.IO
) : BasePushFileRequest(local, remotePath, mode, coroutineContext) {

    override suspend fun handshake(socket: Socket) {
        super.handshake(socket)

        val type = Const.Message.SEND_V1

        val mode = (',' + (mode.toInt(8) and "0777".toInt(8)).toString()).toByteArray(Const.DEFAULT_TRANSPORT_ENCODING)
        val path = remotePath.toByteArray(Const.DEFAULT_TRANSPORT_ENCODING)
        val packetLength = (path.size + mode.size)
        val size = packetLength.toByteArray().reversedArray()

        withDefaultBuffer {
            put(type)
            put(size)
            put(path)
            put(mode)
            compatFlip()
            socket.writeFully(this)
        }
    }
}
