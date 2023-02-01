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

package com.malinskiy.adam.request.shell.v2

import com.malinskiy.adam.Const

data class ShellCommandResult(
    val stdout: ByteArray,
    val stderr: ByteArray,
    val exitCode: Int
) {
    val output: String by lazy {
        String(stdout, Const.DEFAULT_TRANSPORT_ENCODING)
    }

    val errorOutput: String by lazy {
        String(stderr, Const.DEFAULT_TRANSPORT_ENCODING)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ShellCommandResult

        if (!stdout.contentEquals(other.stdout)) return false
        if (!stderr.contentEquals(other.stderr)) return false
        if (exitCode != other.exitCode) return false

        return true
    }

    override fun hashCode(): Int {
        var result = stdout.contentHashCode()
        result = 31 * result + stderr.contentHashCode()
        result = 31 * result + exitCode
        return result
    }
}
