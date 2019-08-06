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

package com.malinskiy.adam.model.cmd.sync

import com.malinskiy.adam.model.cmd.async.LogcatBuffer
import com.malinskiy.adam.model.cmd.async.LogcatFilterSpec
import com.malinskiy.adam.model.cmd.async.LogcatReadMode
import java.time.Instant

class SyncLogcatRequest(
    since: Instant? = null,
    modes: List<LogcatReadMode> = listOf(LogcatReadMode.long),
    buffers: List<LogcatBuffer> = listOf(LogcatBuffer.default),
    pid: Long? = null,
    lastReboot: Boolean? = null,
    filters: List<LogcatFilterSpec> = emptyList()
) : SyncShellCommandRequest<String>(
    cmd = "logcat" +
            " -d" +
            "${
            since?.let {
                " -t ${since.toEpochMilli()}.0"
            } ?: ""
            }" +
            " ${modes.joinToString(separator = " ") { "-v $it" }}" +
            " ${buffers.joinToString(separator = " ") { "-b $it" }}" +
            "${pid?.let { " --pid=$it" } ?: ""}" +
            "${lastReboot?.let { " -L" } ?: ""}" +
            " ${filters.joinToString(separator = " ") { "${it.tag}:${it.level.name}" }}"
                .trimEnd()
) {
    override fun transform(value: String) = value
}