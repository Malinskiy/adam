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

import com.malinskiy.adam.request.transform.ResponseTransformer
import com.malinskiy.adam.transport.Socket
import com.malinskiy.adam.transport.withMaxPacketBuffer
import kotlinx.coroutines.yield

abstract class SynchronousRequest<T : Any?>(target: Target = NonSpecifiedTarget) : ComplexRequest<T>(target), ResponseTransformer<T> {
    override suspend fun readElement(socket: Socket): T {
        withMaxPacketBuffer {
            loop@ do {
                if (socket.isClosedForWrite || socket.isClosedForRead) break@loop

                val data = array()
                val count = socket.readAvailable(data, 0, data.size)
                when {
                    count == 0 -> {
                        yield()
                        continue@loop
                    }
                    count > 0 -> {
                        process(data, 0, count)
                    }
                }
            } while (count >= 0)

            return transform()
        }
    }
}
