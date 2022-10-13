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

package com.malinskiy.adam.request.pkg.multi

import com.malinskiy.adam.exception.RequestRejectedException
import com.malinskiy.adam.extension.readStatus
import com.malinskiy.adam.request.ComplexRequest
import com.malinskiy.adam.request.Feature
import com.malinskiy.adam.request.abb.AbbExecRequest
import com.malinskiy.adam.transport.Socket

class AddSessionRequest(
    private val childSessions: List<String>,
    private val parentSession: String,
    private val supportedFeatures: List<Feature>
) : ComplexRequest<String>() {
    override fun serialize(): ByteArray {
        val hasAbbExec = supportedFeatures.contains(Feature.ABB_EXEC)
        val args = mutableListOf<String>().apply {
            add(
                if (hasAbbExec) {
                    "package"
                } else {
                    "exec:cmd package"
                }
            )
            add("install-add-session")
            add(parentSession)
            addAll(childSessions)
        }.toList()

        return if (hasAbbExec) {
            AbbExecRequest(args, supportedFeatures).serialize()
        } else {
            createBaseRequest(args.joinToString(" "))
        }
    }

    override suspend fun readElement(socket: Socket): String {
        val response = socket.readStatus()
        if (!response.contains("Success")) {
            throw RequestRejectedException(
                "Failed to add child sessions ${childSessions.joinToString()} to a parent session " +
                        "$parentSession: $response"
            )
        }

        return response
    }
}
