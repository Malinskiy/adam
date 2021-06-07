/*
 * Copyright (C) 2019 Anton Malinskiy
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

package com.malinskiy.adam.interactor

import kotlinx.coroutines.delay
import java.io.File

open class AdbBinaryInteractor {
    suspend fun execute(
        adbBinary: File?,
        androidHome: File?,
        vararg cmd: String
    ): Boolean {
        val androidEnvHome: File? = try {
            System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT")
        } catch (e: SecurityException) {
            null
        }?.let { File(it) }

        val os = System.getProperty("os.name").toLowerCase()
        val adbBinaryName = when {
            os.contains("win") -> {
                "adb.exe"
            }
            else -> "adb"
        }

        val adb =
            adbBinary ?: (androidHome ?: androidEnvHome)?.let { File(it, "platform-tools" + File.separator + adbBinaryName) }
            ?: return false
        if (!adb.isFile) return false

        val builder = ProcessBuilder(adb.absolutePath, *cmd).inheritIO()

        val process = builder.start()
        do {
            delay(16)
        } while (process.isAlive)

        return when (process.exitValue()) {
            0 -> true
            else -> false
        }
    }
}
