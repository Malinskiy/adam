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

package com.malinskiy.adam.model.cmd

import com.malinskiy.adam.Const
import java.io.UnsupportedEncodingException

open abstract class Request {
    abstract fun serialize(): ByteArray

    /**
     * If this throws [UnsupportedEncodingException] then all is doomed:
     * we can't communicate with the adb server so propagating the exception up
     */
    @Throws(UnsupportedEncodingException::class)
    protected fun createBaseRequest(request: String): ByteArray {
        return String.format("%04X%s", request.length, request)
            .toByteArray(Const.DEFAULT_TRANSPORT_ENCODING)
    }
}