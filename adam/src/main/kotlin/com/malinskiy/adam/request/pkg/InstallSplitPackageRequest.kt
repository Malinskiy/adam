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

/*
 * Copyright (C) 2020 Anton Malinskiy
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
import com.malinskiy.adam.request.pkg.multi.ApkSplitInstallationPackage
import com.malinskiy.adam.request.pkg.multi.CreateIndividualPackageSessionRequest
import com.malinskiy.adam.request.pkg.multi.InstallCommitRequest
import com.malinskiy.adam.request.pkg.multi.WriteIndividualPackageRequest
import kotlinx.coroutines.Dispatchers
import java.io.File
import kotlin.coroutines.CoroutineContext

/**
 * If both CMD and ABB_EXEC are missing, falls back to exec:pm
 */
@Features(Feature.CMD, Feature.ABB_EXEC)
class InstallSplitPackageRequest(
    private val pkg: ApkSplitInstallationPackage,
    private val supportedFeatures: List<Feature>,
    private val reinstall: Boolean,
    private val extraArgs: List<String> = emptyList(),
    val coroutineContext: CoroutineContext = Dispatchers.IO
) : AccumulatingMultiRequest<String>() {

    private val totalSize: Long by lazy {
        pkg.fileList.sumOf { it.length() }
    }

    override suspend fun execute(androidDebugBridgeClient: AndroidDebugBridgeClient, serial: String?) = with(androidDebugBridgeClient) {
        val (sessionId, output) = execute(
            CreateIndividualPackageSessionRequest(
                pkg,
                listOf(pkg),
                supportedFeatures,
                reinstall,
                extraArgs + "-S${totalSize}"
            ),
            serial
        )
        output.addToResponse()

        try {
            for (file in pkg.fileList) {
                execute(WriteIndividualPackageRequest(file, supportedFeatures, sessionId, coroutineContext), serial).addToResponse()
            }
            execute(InstallCommitRequest(sessionId, supportedFeatures), serial).addToResponse()

            accumulatedResponse
        } catch (e: Exception) {
            try {
                execute(InstallCommitRequest(sessionId, supportedFeatures, abandon = true), serial)
            } catch (e: Exception) {
                //Ignore
            }
            throw e
        }
    }

    override fun validate(): ValidationResponse {
        val response = super.validate()
        if (!response.success) {
            return response
        } else {
            for (file in pkg.fileList) {
                val message = validateFile(file) ?: continue
                return ValidationResponse(false, message)
            }

            if (!pkg.fileList.any { it.extension == "apk" }) {
                return ValidationResponse(false, ValidationResponse.oneOfFilesShouldBe("apk"))
            }
        }

        return ValidationResponse.Success
    }

    private fun validateFile(file: File): String? {
        return if (!file.exists()) {
            ValidationResponse.packageShouldExist(file)
        } else if (!file.isFile) {
            ValidationResponse.packageShouldBeRegularFile(file)
        } else if (file.extension == "apex") {
            "Apex is not compatible with InstallSplitPackageRequest"
        } else if (!SUPPORTED_EXTENSIONS.contains(file.extension)) {
            ValidationResponse.packageShouldBeSupportedExtension(file, SUPPORTED_EXTENSIONS)
        } else {
            null
        }
    }

    companion object {
        val SUPPORTED_EXTENSIONS = setOf("apk", "dm", "fsv_sig")
    }
}
