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
import java.util.*

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

        val os = System.getProperty("os.name").lowercase(Locale.ENGLISH)
        val isWindows = os.contains("win")
        val adbBinaryName = when {
            isWindows -> {
                "adb.exe"
            }
            else -> "adb"
        }

        val adb = when {
            adbBinary != null -> adbBinary
            androidHome != null -> File(androidHome, "platform-tools" + File.separator + adbBinaryName)
            androidEnvHome != null -> File(androidEnvHome, "platform-tools" + File.separator + adbBinaryName)
            else -> discoverAdbBinary(isWindows)
        }
        if (adb?.isFile != true) return false

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

    private fun discoverAdbBinary(isWindows: Boolean): File? {
        val discoverCommand = if (isWindows) "where" else "which"
        val builder = ProcessBuilder(discoverCommand, "adb").inheritIO()
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
        val process = builder.start()
        process.waitFor()

        return process.takeIf { it.exitValue() == 0 }
            ?.inputStream
            ?.bufferedReader()
            ?.readLine()
            ?.let(::File)
    }
}
