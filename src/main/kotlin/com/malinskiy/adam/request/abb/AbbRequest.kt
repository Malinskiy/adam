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

package com.malinskiy.adam.request.abb

import com.malinskiy.adam.annotation.RequiresFeatures
import com.malinskiy.adam.request.Feature
import com.malinskiy.adam.request.abb.AbbExecRequest.Companion.DELIMITER
import com.malinskiy.adam.request.shell.v2.ShellCommandResult
import com.malinskiy.adam.request.shell.v2.SyncShellCommandRequest

@RequiresFeatures(Feature.ABB)
class AbbRequest(private val args: List<String>) : SyncShellCommandRequest<ShellCommandResult>("") {
    override fun serialize() = createBaseRequest("abb:${args.joinToString(DELIMITER.toString())}")
    override fun convertResult(response: ShellCommandResult) = response
}