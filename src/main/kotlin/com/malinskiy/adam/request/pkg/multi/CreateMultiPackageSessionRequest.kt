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

import com.malinskiy.adam.annotation.Features
import com.malinskiy.adam.exception.RequestRejectedException
import com.malinskiy.adam.extension.bashEscape
import com.malinskiy.adam.request.ComplexRequest
import com.malinskiy.adam.request.Feature
import com.malinskiy.adam.request.ValidationResponse
import com.malinskiy.adam.request.abb.AbbExecRequest
import com.malinskiy.adam.transport.AndroidReadChannel
import com.malinskiy.adam.transport.AndroidWriteChannel
import java.io.File

@Features(Feature.CMD, Feature.ABB_EXEC)
class CreateMultiPackageSessionRequest(
    private val pkgList: List<InstallationPackage>,
    private val supportedFeatures: List<Feature>,
    private val reinstall: Boolean,
    private val extraArgs: List<String> = emptyList()
) : ComplexRequest<String>() {
    override fun validate(): ValidationResponse {
        loop@ for (installationPackage in pkgList) {
            when (installationPackage) {
                is SingleFileInstallationPackage -> {
                    val message = validateFile(installationPackage.file) ?: continue@loop
                    return ValidationResponse(false, message)
                }
                is ApkSplitInstallationPackage -> {
                    val fileList = installationPackage.fileList
                    splitLoop@ for (file in fileList) {
                        val message = validateFile(file) ?: continue@splitLoop
                        return ValidationResponse(false, message)
                    }
                }
            }
        }

        return ValidationResponse.Success
    }

    private fun validateFile(file: File): String? {
        return if (!file.exists()) {
            "Package ${file.absolutePath} doesn't exist"
        } else if (!file.isFile) {
            "Package ${file.absolutePath} is not a regular file"
        } else if (!supportedFeatures.contains(Feature.ABB_EXEC) && !supportedFeatures.contains(Feature.CMD)) {
            "Supported features must include either ABB_EXEC or CMD"
        } else if (file.extension == "apex" && !supportedFeatures.contains(Feature.APEX)) {
            "Apex is not supported by this device"
        } else if (file.extension != "apk") {
            "Unsupported package extension ${file.extension}. Should be either apk or apex"
        } else {
            null
        }
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

            add("install-create")
            add("--multi-package")

            if (hasAbbExec) {
                addAll(extraArgs)
            } else if (extraArgs.isNotEmpty()) {
                add(extraArgs.bashEscape())
            }

            if (reinstall) {
                add("-r")
            }

            if (pkgList.map {
                    when (it) {
                        is SingleFileInstallationPackage -> listOf(it.file)
                        is ApkSplitInstallationPackage -> it.fileList
                    }
                }
                    .flatten()
                    .any { it.extension == "apex" }
            ) {
                add("--staged")
            }
        }.toList()

        return if (hasAbbExec) {
            AbbExecRequest(args).serialize()
        } else {
            createBaseRequest(args.joinToString(" "))
        }
    }

    override suspend fun readElement(readChannel: AndroidReadChannel, writeChannel: AndroidWriteChannel): String {
        val createSessionResponse = readChannel.readStatus()
        if (!createSessionResponse.contains("Success")) {
            throw RequestRejectedException("Failed to create multi-package session")
        }

        val sessionId = createSessionResponse.substringAfter('[', "").substringBefore(']', "")
        if (sessionId.isEmpty()) {
            throw RequestRejectedException("Failed to create multi-package session")
        }

        return sessionId
    }
}
