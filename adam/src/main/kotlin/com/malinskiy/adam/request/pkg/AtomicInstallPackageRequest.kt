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

import com.malinskiy.adam.AndroidDebugBridgeClient
import com.malinskiy.adam.annotation.Features
import com.malinskiy.adam.request.AccumulatingMultiRequest
import com.malinskiy.adam.request.Feature
import com.malinskiy.adam.request.ValidationResponse
import com.malinskiy.adam.request.pkg.multi.AddSessionRequest
import com.malinskiy.adam.request.pkg.multi.ApkSplitInstallationPackage
import com.malinskiy.adam.request.pkg.multi.CreateIndividualPackageSessionRequest
import com.malinskiy.adam.request.pkg.multi.CreateMultiPackageSessionRequest
import com.malinskiy.adam.request.pkg.multi.InstallCommitRequest
import com.malinskiy.adam.request.pkg.multi.InstallationPackage
import com.malinskiy.adam.request.pkg.multi.SingleFileInstallationPackage
import com.malinskiy.adam.request.pkg.multi.WriteIndividualPackageRequest
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

/**
 * Push one or more packages to the device and install them atomically
 *
 * Requires Feature.CMD support on the target device
 * Even if CMD is supported, there is no promise that `cmd package install --multi-package`
 * is available: there were version that didn't have this mode
 *
 * Optionally uses Feature.ABB_EXEC
 * Support for Feature.APEX is required for installing apex packages
 *
 *
 * @see com.malinskiy.adam.request.device.FetchDeviceFeaturesRequest
 */
@Features(Feature.CMD, Feature.ABB_EXEC, Feature.APEX)
class AtomicInstallPackageRequest(
    private val pkgList: List<InstallationPackage>,
    private val supportedFeatures: List<Feature>,
    private val reinstall: Boolean,
    private val extraArgs: List<String> = emptyList(),
    val coroutineContext: CoroutineContext = Dispatchers.IO
) : AccumulatingMultiRequest<String>() {
    override suspend fun execute(androidDebugBridgeClient: AndroidDebugBridgeClient, serial: String?) = with(androidDebugBridgeClient) {
        val (parentSessionId, output) =
            execute(CreateMultiPackageSessionRequest(pkgList, supportedFeatures, reinstall, extraArgs), serial)
        output.addToResponse()

        try {
            val childSessionIds = mutableListOf<String>()
            for (pkg in pkgList) {
                val (childSessionId, output) =
                    execute(
                        CreateIndividualPackageSessionRequest(pkg, pkgList, supportedFeatures, reinstall, extraArgs),
                        serial
                    )
                output.addToResponse()

                when (pkg) {
                    is SingleFileInstallationPackage -> {
                        execute(
                            WriteIndividualPackageRequest(pkg.file, supportedFeatures, childSessionId, coroutineContext),
                            serial
                        ).addToResponse()
                    }

                    is ApkSplitInstallationPackage -> {
                        for (file in pkg.fileList) {
                            execute(
                                WriteIndividualPackageRequest(file, supportedFeatures, childSessionId, coroutineContext),
                                serial
                            ).addToResponse()
                        }
                    }
                }
                childSessionIds.add(childSessionId)
            }

            execute(AddSessionRequest(childSessionIds, parentSessionId, supportedFeatures), serial).addToResponse()
            execute(InstallCommitRequest(parentSessionId, supportedFeatures), serial).addToResponse()

            accumulatedResponse
        } catch (e: Exception) {
            try {
                execute(InstallCommitRequest(parentSessionId, supportedFeatures, abandon = true), serial)
            } catch (e: Exception) {
                //Ignore
            }
            throw e
        }
    }

    override fun validate(): ValidationResponse {
        val response = super.validate()
        return if (!response.success) {
            response
        } else if (!supportedFeatures.contains(Feature.CMD) && !supportedFeatures.contains(Feature.ABB_EXEC)) {
            ValidationResponse(false, ValidationResponse.missingEitherFeature(Feature.ABB_EXEC, Feature.CMD))
        } else if (pkgList.map {
                when (it) {
                    is SingleFileInstallationPackage -> listOf(it.file)
                    is ApkSplitInstallationPackage -> it.fileList
                }
            }.flatten().any { file -> file.extension == "apex" } && !supportedFeatures.contains(Feature.APEX)) {
            ValidationResponse(false, ValidationResponse.missingFeature(Feature.APEX))
        } else {
            ValidationResponse.Success
        }
    }
}
