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
import com.malinskiy.adam.extension.bashEscape
import com.malinskiy.adam.extension.readStatus
import com.malinskiy.adam.request.ComplexRequest
import com.malinskiy.adam.request.Feature
import com.malinskiy.adam.request.abb.AbbExecRequest
import com.malinskiy.adam.transport.Socket

class CreateIndividualPackageSessionRequest(
    private val pkg: InstallationPackage,
    private val pkgList: List<InstallationPackage>,
    private val supportedFeatures: List<Feature>,
    private val reinstall: Boolean,
    private val extraArgs: List<String> = emptyList()
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

            add("install-create")

            if (hasAbbExec) {
                addAll(extraArgs)
            } else if (extraArgs.isNotEmpty()) {
                addAll(extraArgs.map { it.bashEscape() }.toList())
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

            if (when (pkg) {
                    is SingleFileInstallationPackage -> listOf(pkg.file)
                    is ApkSplitInstallationPackage -> pkg.fileList
                }.any { it.extension == "apex" }
            ) {
                add("--apex")
            }
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
            throw RequestRejectedException("Failed to create multi-package session: $response")
        }

        val sessionId = response.substringAfter('[', "").substringBefore(']', "")
        if (sessionId.isEmpty()) {
            throw RequestRejectedException("Failed to create multi-package session")
        }

        return sessionId
    }
}
