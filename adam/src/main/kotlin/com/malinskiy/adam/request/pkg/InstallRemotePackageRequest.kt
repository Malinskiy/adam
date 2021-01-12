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

import com.malinskiy.adam.request.ValidationResponse
import com.malinskiy.adam.request.shell.v1.ShellCommandRequest

class InstallRemotePackageRequest(
    private val absoluteRemoteFilePath: String,
    reinstall: Boolean,
    extraArgs: List<String> = emptyList()
) : ShellCommandRequest(
    cmd = StringBuilder().apply {
        append("pm install ")

        if (reinstall) {
            append("-r ")
        }

        if (extraArgs.isNotEmpty()) {
            append(extraArgs.joinToString(" "))
            append(" ")
        }

        append(absoluteRemoteFilePath)
    }.toString()
) {
    override fun validate(): ValidationResponse {
        val result = super.validate()
        return if (!result.success) {
            result
        } else if (absoluteRemoteFilePath.endsWith(".apex")) {
            ValidationResponse(false, "APEX packages are only not compatible with InstallRemotePackageRequest")
        } else if (!absoluteRemoteFilePath.endsWith(".apk")) {
            ValidationResponse(false, "Unsupported package extension ${absoluteRemoteFilePath.substringAfterLast('.')}. Should be apk")
        } else {
            ValidationResponse.Success
        }
    }
}
