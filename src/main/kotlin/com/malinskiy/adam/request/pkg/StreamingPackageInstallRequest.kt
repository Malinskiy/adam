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

import com.malinskiy.adam.annotation.Features
import com.malinskiy.adam.extension.bashEscape
import com.malinskiy.adam.extension.copyTo
import com.malinskiy.adam.io.AsyncFileReader
import com.malinskiy.adam.io.copyTo
import com.malinskiy.adam.request.ComplexRequest
import com.malinskiy.adam.request.Feature
import com.malinskiy.adam.request.ValidationResponse
import com.malinskiy.adam.request.abb.AbbExecRequest
import com.malinskiy.adam.request.transform.StringResponseTransformer
import com.malinskiy.adam.transport.Socket
import com.malinskiy.adam.transport.withDefaultBuffer
import kotlinx.coroutines.Dispatchers
import java.io.File
import kotlin.coroutines.CoroutineContext

/**
 * This request requires either Feature.CMD or Feature.ABB_EXEC support on the target device
 * Additionally, support for Feature.APEX is required for installing apex packages
 *
 * @see com.malinskiy.adam.request.device.FetchDeviceFeaturesRequest
 */
@Features(Feature.CMD, Feature.ABB_EXEC, Feature.APEX)
class StreamingPackageInstallRequest(
    private val pkg: File,
    private val supportedFeatures: List<Feature>,
    private val reinstall: Boolean,
    private val extraArgs: List<String> = emptyList(),
    val coroutineContext: CoroutineContext = Dispatchers.IO
) : ComplexRequest<Boolean>() {
    private val transformer = StringResponseTransformer()

    override fun validate(): ValidationResponse {
        val response = super.validate()
        if (!response.success) {
            return response
        }

        val message =
            if (!pkg.exists()) {
                ValidationResponse.packageShouldExist(pkg)
            } else if (!pkg.isFile) {
                ValidationResponse.packageShouldBeRegularFile(pkg)
            } else if (!supportedFeatures.contains(Feature.ABB_EXEC) && !supportedFeatures.contains(Feature.CMD)) {
                ValidationResponse.missingEitherFeature(Feature.ABB_EXEC, Feature.CMD)
            } else if (pkg.extension == "apex" && !supportedFeatures.contains(Feature.APEX)) {
                ValidationResponse.missingFeature(Feature.APEX)
            } else if (!SUPPORTED_EXTENSIONS.contains(pkg.extension)) {
                ValidationResponse.packageShouldBeSupportedExtension(pkg, SUPPORTED_EXTENSIONS)
            } else {
                null
            }

        return ValidationResponse(message == null, message)
    }

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

            add("install")

            if (hasAbbExec) {
                addAll(extraArgs)
            } else if (extraArgs.isNotEmpty()) {
                add(extraArgs.bashEscape())
            }

            if (reinstall) {
                add("-r")
            }

            add("-S")
            add("${pkg.length()}")
            if (pkg.extension == "apex") {
                add("--apex")
            }
        }.toList()

        return if (hasAbbExec) {
            AbbExecRequest(args, supportedFeatures).serialize()
        } else {
            createBaseRequest(args.joinToString(" "))
        }
    }

    override suspend fun readElement(socket: Socket): Boolean {
        AsyncFileReader(pkg, coroutineContext = coroutineContext).use { reader ->
            reader.start()
            reader.copyTo(socket)
        }

        withDefaultBuffer {
            socket.copyTo(transformer, this)
        }
        return transformer.transform().startsWith("Success")
    }

    companion object {
        val SUPPORTED_EXTENSIONS = setOf("apk", "apex")
    }
}
