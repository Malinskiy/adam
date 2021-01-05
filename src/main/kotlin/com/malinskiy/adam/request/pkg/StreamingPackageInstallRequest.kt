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

import com.malinskiy.adam.Const
import com.malinskiy.adam.annotation.Features
import com.malinskiy.adam.extension.bashEscape
import com.malinskiy.adam.extension.copyTo
import com.malinskiy.adam.request.ComplexRequest
import com.malinskiy.adam.request.Feature
import com.malinskiy.adam.request.ValidationResponse
import com.malinskiy.adam.request.abb.AbbExecRequest
import com.malinskiy.adam.request.transform.StringResponseTransformer
import com.malinskiy.adam.transport.AndroidReadChannel
import com.malinskiy.adam.transport.AndroidWriteChannel
import io.ktor.util.cio.*
import io.ktor.utils.io.*
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
        val message =
            if (!pkg.exists()) {
                "Package ${pkg.absolutePath} doesn't exist"
            } else if (!pkg.isFile) {
                "Package ${pkg.absolutePath} is not a regular file"
            } else if (!supportedFeatures.contains(Feature.ABB_EXEC) && !supportedFeatures.contains(Feature.CMD)) {
                "Supported features must include either ABB_EXEC or CMD"
            } else if (pkg.extension == "apex" && !supportedFeatures.contains(Feature.APEX)) {
                "Apex is not supported by this device"
            } else if (pkg.extension != "apk") {
                "Unsupported package extension ${pkg.extension}. Should be either apk or apex"
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
            AbbExecRequest(args).serialize()
        } else {
            createBaseRequest(args.joinToString(" "))
        }
    }

    override suspend fun readElement(readChannel: AndroidReadChannel, writeChannel: AndroidWriteChannel): Boolean {
        val buffer = ByteArray(Const.MAX_FILE_PACKET_LENGTH)
        var fileChannel: ByteReadChannel? = null
        try {
            val fileChannel = pkg.readChannel(coroutineContext = coroutineContext)
            fileChannel.copyTo(writeChannel, buffer)
        } finally {
            fileChannel?.cancel()
        }

        readChannel.copyTo(transformer, buffer)
        return transformer.transform().startsWith("Success")
    }
}
