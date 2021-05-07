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

package com.malinskiy.adam.transport

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

interface SuspendCloseable {
    suspend fun close()
}

suspend inline fun <C : SuspendCloseable, R> C.use(block: suspend (C) -> R): R {
    var closed = false

    return try {
        block(this)
    } catch (first: Throwable) {
        try {
            closed = true
            withContext(NonCancellable) {
                println("Closing suspend closeable due to exception")
                first.printStackTrace()
                close()
            }
        } catch (second: Throwable) {
            try {
                Throwable::class.java.getMethod("addSuppressed", Throwable::class.java).invoke(this, second)
            } catch (t: Throwable) {
                null
            }
        }

        throw first
    } finally {
        if (!closed) {
            withContext(NonCancellable) {
                close()
            }
        }
    }
}
