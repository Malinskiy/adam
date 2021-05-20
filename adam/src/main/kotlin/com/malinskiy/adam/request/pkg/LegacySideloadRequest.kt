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

package com.malinskiy.adam.request.pkg

import com.malinskiy.adam.extension.readTransportResponse
import com.malinskiy.adam.io.AsyncFileReader
import com.malinskiy.adam.io.copyTo
import com.malinskiy.adam.request.ComplexRequest
import com.malinskiy.adam.request.ValidationResponse
import com.malinskiy.adam.transport.Socket
import com.malinskiy.adam.transport.use
import kotlinx.coroutines.Dispatchers
import java.io.File
import kotlin.coroutines.CoroutineContext

/**
 * pre-KitKat
 */
class LegacySideloadRequest(
    private val pkg: File,
    val coroutineContext: CoroutineContext = Dispatchers.IO
) : ComplexRequest<Boolean>() {
    override fun validate(): ValidationResponse {
        val message =
            if (!pkg.exists()) {
                ValidationResponse.packageShouldExist(pkg)
            } else if (!pkg.isFile) {
                ValidationResponse.packageShouldBeRegularFile(pkg)
            } else {
                null
            }

        return ValidationResponse(message == null, message)
    }

    override fun serialize() = createBaseRequest("sideload:${pkg.length()}")

    override suspend fun readElement(socket: Socket): Boolean {
        AsyncFileReader(pkg, coroutineContext = coroutineContext).use { reader ->
            reader.start()
            reader.copyTo(socket)
        }
        return socket.readTransportResponse().okay
    }
}
