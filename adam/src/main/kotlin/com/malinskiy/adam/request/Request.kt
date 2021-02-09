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
import com.malinskiy.adam.exception.RequestRejectedException
import com.malinskiy.adam.extension.readTransportResponse
import com.malinskiy.adam.extension.write
import com.malinskiy.adam.log.AdamLogging
import com.malinskiy.adam.transport.Socket
import java.io.UnsupportedEncodingException

/**
 * By default all requests are targeted at adb daemon itself
 * @see [Target]
 *
 * @param socketIdleTimeout override for socket idle timeout
 */
open abstract class Request(
    val target: Target = HostTarget,
    val socketIdleTimeout: Long? = null
) {

    /**
     * Some requests require a device serial to be passed to the request itself by means of <host-prefix>
     * @see https://android.googlesource.com/platform/system/core/+/refs/heads/master/adb/SERVICES.TXT
     */
    abstract fun serialize(): ByteArray

    open suspend fun handshake(socket: Socket) {
        val request = serialize()
        socket.write(request)
        val response = socket.readTransportResponse()
        if (!response.okay) {
            log.warn { "adb server rejected command ${String(request, Const.DEFAULT_TRANSPORT_ENCODING)}" }
            throw RequestRejectedException(response.message ?: "no message received")
        }
    }

    /**
     * If this throws [UnsupportedEncodingException] then all is doomed:
     * we can't communicate with the adb server so propagating the exception up
     */
    @Throws(UnsupportedEncodingException::class)
    protected fun createBaseRequest(request: String): ByteArray {
        val fullRequest = target.serialize() + request
        val result = String.format("%04X%s", fullRequest.length, fullRequest)
            .toByteArray(Const.DEFAULT_TRANSPORT_ENCODING)

        //Need to set proper length in case there are multi-byte characters
        val actualLength = result.size - 4
        String.format("%04X", actualLength).toByteArray(Const.DEFAULT_TRANSPORT_ENCODING).copyInto(result, 0, 0, 4)

        return result
    }

    open fun validate(): ValidationResponse = ValidationResponse.Success

    companion object {
        private val log = AdamLogging.logger {}
    }
}
