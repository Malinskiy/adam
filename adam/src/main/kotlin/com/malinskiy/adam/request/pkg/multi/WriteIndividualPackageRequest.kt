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
import com.malinskiy.adam.extension.writeFile
import com.malinskiy.adam.request.ComplexRequest
import com.malinskiy.adam.request.Feature
import com.malinskiy.adam.request.abb.AbbExecRequest
import com.malinskiy.adam.transport.Socket
import kotlinx.coroutines.Dispatchers
import java.io.File
import kotlin.coroutines.CoroutineContext

class WriteIndividualPackageRequest(
    private val file: File,
    private val supportedFeatures: List<Feature>,
    private val session: String,
    private val coroutineContext: CoroutineContext = Dispatchers.IO
) : ComplexRequest<String>() {
    override fun serialize(): ByteArray {
        val hasAbbExec = supportedFeatures.contains(Feature.ABB_EXEC)
        val args = mutableListOf<String>().apply {
            add(
                when {
                    supportedFeatures.contains(Feature.ABB_EXEC) -> "package"
                    supportedFeatures.contains(Feature.CMD) -> "exec:cmd package"
                    //User is responsible for checking if pm supports install-write in this case
                    else -> "exec:pm"
                }
            )

            add("install-write")
            add("-S")
            add(file.length().toString())
            add(session)
            add("${file.name}")
            add("-")
        }.toList()

        return if (hasAbbExec) {
            AbbExecRequest(args, supportedFeatures).serialize()
        } else {
            createBaseRequest(args.joinToString(" "))
        }
    }

    override suspend fun readElement(socket: Socket): String {
        socket.writeFile(file, coroutineContext)
        val response = socket.readStatus()
        if (!response.contains("Success")) {
            throw RequestRejectedException("Failed to write package $file: $response")
        }

        return response
    }
}
