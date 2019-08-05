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

package com.malinskiy.adam.transport

import com.malinskiy.adam.model.cmd.Request
import kotlinx.coroutines.io.ByteWriteChannel
import java.nio.ByteBuffer

class AndroidWriteChannel(private val delegate: ByteWriteChannel) : ByteWriteChannel by delegate {
    suspend fun write(request: Request, length: Int? = null) {
        val bytes = request.serialize()
        val requestBuffer = ByteBuffer.wrap(bytes, 0, length ?: bytes.size)
        delegate.writeFully(requestBuffer)
    }
}