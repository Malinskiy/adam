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

import com.malinskiy.adam.exception.RequestRejectedException
import com.malinskiy.adam.extension.readOptionalProtocolString
import com.malinskiy.adam.request.ComplexRequest
import com.malinskiy.adam.request.HostTarget
import com.malinskiy.adam.transport.Socket

/**
 * @see https://android.googlesource.com/platform/system/core/+/refs/heads/master/adb/adb.h#62
 */
class GetAdbServerVersionRequest : ComplexRequest<Int>(target = HostTarget) {
    override suspend fun readElement(socket: Socket): Int {
        val version = socket.readOptionalProtocolString()
        return version?.toIntOrNull(radix = 16) ?: throw RequestRejectedException("Empty/corrupt response")
    }

    override fun serialize() = createBaseRequest("version")
}
