/*
 * Copyright (C) 2022 Anton Malinskiy
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

package com.malinskiy.adam.extension

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import java.io.File

fun CoroutineScope.sequentialRead(file: File, sizeChannel: ReceiveChannel<Int>): ReceiveChannel<ByteArray> = produce(capacity = 1) {
    file.inputStream().buffered().use { stream ->
        var position = 0
        for (size in sizeChannel) {
            val buffer = ByteArray(size)
            val read = stream.read(buffer)
            if (read == -1) {
                close()
                break
            } else if (read == buffer.size) {
                send(buffer)
            } else {
                send(buffer.copyOf(read))
            }
            position += read
            println("$position/${file.length()}")
        }
    }
}
