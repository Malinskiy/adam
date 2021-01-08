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

package com.malinskiy.adam.request.abb

import com.malinskiy.adam.annotation.Features
import com.malinskiy.adam.request.ComplexRequest
import com.malinskiy.adam.request.Feature
import com.malinskiy.adam.request.ValidationResponse
import com.malinskiy.adam.transport.AndroidReadChannel
import com.malinskiy.adam.transport.AndroidWriteChannel

@Features(Feature.ABB_EXEC)
open class AbbExecRequest(private val args: List<String>, private val supportedFeatures: List<Feature>) : ComplexRequest<Unit>() {
    override suspend fun readElement(readChannel: AndroidReadChannel, writeChannel: AndroidWriteChannel) = Unit

    override fun serialize() = createBaseRequest("abb_exec:${args.joinToString(DELIMITER.toString())}")

    override fun validate(): ValidationResponse {
        val response = super.validate()
        return if (!response.success) {
            response
        } else if (!supportedFeatures.contains(Feature.ABB_EXEC)) {
            ValidationResponse(false, ValidationResponse.missingFeature(Feature.ABB_EXEC))
        } else {
            ValidationResponse.Success
        }
    }

    companion object {
        const val DELIMITER = 0x00.toChar()
    }
}
