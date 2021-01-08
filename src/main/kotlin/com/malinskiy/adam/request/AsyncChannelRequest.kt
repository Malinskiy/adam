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

package com.malinskiy.adam.request

import com.malinskiy.adam.transport.AndroidReadChannel
import com.malinskiy.adam.transport.AndroidWriteChannel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel

/**
 * Read and write are called in sequence, hence you have to give the control flow back if you want
 * cooperative multitasking to happen
 */
abstract class AsyncChannelRequest<T : Any?, I : Any?>(
    val channel: ReceiveChannel<I>? = null,
    target: Target = NonSpecifiedTarget
) : Request(target) {

    /**
     * Called after the initial OKAY confirmation
     */
    abstract suspend fun readElement(readChannel: AndroidReadChannel, writeChannel: AndroidWriteChannel): T?

    /**
     * Called after each readElement
     */
    abstract suspend fun writeElement(element: I, readChannel: AndroidReadChannel, writeChannel: AndroidWriteChannel)

    /**
     * Optionally send a message
     * The transport connection is not available at this point
     */
    open fun close(channel: SendChannel<T>) = Unit
}
