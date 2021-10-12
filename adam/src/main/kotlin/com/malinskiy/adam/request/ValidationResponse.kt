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

package com.malinskiy.adam.request

import com.malinskiy.adam.Const
import java.io.File
import java.util.*

data class ValidationResponse(
    val success: Boolean,
    val message: String?
) {
    companion object {
        val Success = ValidationResponse(true, null)

        fun missingFeature(feature: Feature) = "${feature.name} is not supported by device"
        fun missingEitherFeature(vararg feature: Feature) = "Supported features must include either of ${feature.joinToString()}"
        fun oneOfFilesShouldBe(extension: String) = "At least one of the files has to be an ${extension.uppercase(Locale.ENGLISH)} file"
        fun packageShouldExist(file: File) = "Package ${file.absolutePath} doesn't exist"
        fun packageShouldBeRegularFile(file: File) = "Package ${file.absolutePath} is not a regular file"
        fun packageShouldBeSupportedExtension(file: File, supported: Set<String>) =
            "Unsupported package extension ${file.extension}. Should be on of ${supported.joinToString()}}"

        fun pathShouldNotBeLong() = "Remote path should be less that ${Const.MAX_REMOTE_PATH_LENGTH} bytes"
    }
}
