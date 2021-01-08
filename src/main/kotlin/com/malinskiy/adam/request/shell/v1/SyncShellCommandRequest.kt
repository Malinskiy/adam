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

package com.malinskiy.adam.request.shell.v1

import com.malinskiy.adam.request.NonSpecifiedTarget
import com.malinskiy.adam.request.SynchronousRequest
import com.malinskiy.adam.request.Target

/**
 * shell v1 service doesn't support exit codes
 * we append an `echo $?` at the end to print the exit code as well and parse it
 */
abstract class SyncShellCommandRequest<T : Any?>(val cmd: String, target: Target = NonSpecifiedTarget) :
    SynchronousRequest<T>(target) {

    /**
     * Descendants should override this method to map the response to appropriate output
     */
    abstract fun convertResult(response: ShellCommandResult): T

    private val responseTransformer = ShellResultResponseTransformer()
    override suspend fun process(bytes: ByteArray, offset: Int, limit: Int) = responseTransformer.process(bytes, offset, limit)
    override fun transform() = convertResult(responseTransformer.transform())
    override fun serialize() = createBaseRequest("shell:$cmd;echo ${EXIT_CODE_DELIMITER}$?")

    companion object {
        const val EXIT_CODE_DELIMITER = 'x'
    }
}