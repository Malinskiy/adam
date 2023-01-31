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

package com.malinskiy.adam.request.shell.v1

import com.google.common.io.ByteStreams
import com.malinskiy.adam.Const
import com.malinskiy.adam.exception.RequestRejectedException
import com.malinskiy.adam.request.transform.ResponseTransformer

class ShellResultResponseTransformer : ResponseTransformer<ShellCommandResult> {
    override suspend fun process(bytes: ByteArray, offset: Int, limit: Int) {
        builder.write(bytes, 0, limit)
    }

    private val builder = ByteStreams.newDataOutput()

    override fun transform(): ShellCommandResult {
        val output = String(builder.toByteArray(), Const.DEFAULT_TRANSPORT_ENCODING)
        val indexOfDelimiter = output.lastIndexOf(SyncShellCommandRequest.EXIT_CODE_DELIMITER)
        if (indexOfDelimiter == -1) {
            throw RequestRejectedException("No exit code delimiter found in $output")
        }
        val stdout = output.substring(0 until indexOfDelimiter)
        val exitCodeString = output.substring(indexOfDelimiter + 1).trim()
        val exitCode = exitCodeString.toIntOrNull() ?: throw RequestRejectedException("Unexpected exit code value $exitCodeString")
        return ShellCommandResult(
            stdout = stdout.toByteArray(Const.DEFAULT_TRANSPORT_ENCODING),
            exitCode = exitCode
        )
    }
}
