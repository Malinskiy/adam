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
import com.malinskiy.adam.request.ComplexRequest
import com.malinskiy.adam.request.Feature
import com.malinskiy.adam.request.abb.AbbExecRequest
import com.malinskiy.adam.transport.AndroidReadChannel
import com.malinskiy.adam.transport.AndroidWriteChannel

class InstallCommitRequest(
    private val parentSession: String,
    private val supportedFeatures: List<Feature>,
    private val abandon: Boolean = false
) : ComplexRequest<Unit>() {
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

            if (abandon) {
                add("install-abandon")
            } else {
                add("install-commit")
            }

            add(parentSession)
        }.toList()

        return if (hasAbbExec) {
            AbbExecRequest(args).serialize()
        } else {
            createBaseRequest(args.joinToString(" "))
        }
    }

    override suspend fun readElement(readChannel: AndroidReadChannel, writeChannel: AndroidWriteChannel) {
        val result = readChannel.readStatus()
        if (!result.contains("Success")) {
            throw RequestRejectedException("Failed to finalize session $parentSession: $result")
        }
    }
}
