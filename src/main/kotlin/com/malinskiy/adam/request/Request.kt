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

import com.malinskiy.adam.Const
import java.io.UnsupportedEncodingException

open abstract class Request(val target: Target = Host) {

    /**
     * Some requests require a device serial to be passed to the request itself by means of <host-prefix>
     * @see https://android.googlesource.com/platform/system/core/+/refs/heads/master/adb/SERVICES.TXT
     */
    abstract fun serialize(): ByteArray

    /**
     * If this throws [UnsupportedEncodingException] then all is doomed:
     * we can't communicate with the adb server so propagating the exception up
     */
    @Throws(UnsupportedEncodingException::class)
    protected fun createBaseRequest(request: String): ByteArray {
        val fullRequest = target.serialize() + request
        return String.format("%04X%s", fullRequest.length, fullRequest)
            .toByteArray(Const.DEFAULT_TRANSPORT_ENCODING)
    }
}