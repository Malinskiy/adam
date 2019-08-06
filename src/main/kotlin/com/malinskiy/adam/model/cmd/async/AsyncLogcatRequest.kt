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

package com.malinskiy.adam.model.cmd.async

import java.time.Instant

class LogcatRequestAsync(
    since: Instant? = null,
    modes: List<LogcatReadMode> = listOf(LogcatReadMode.long),
    buffers: List<LogcatBuffer> = listOf(LogcatBuffer.default),
    pid: Long? = null,
    lastReboot: Boolean? = null,
    filters: List<LogcatFilterSpec> = emptyList()
) : AsyncShellCommandRequest(
    cmd = "logcat" +
            "${
            since?.let {
                " -T ${since.toEpochMilli()}.0"
            } ?: ""
            }" +
            " ${modes.joinToString(separator = " ") { "-v $it" }}" +
            " ${buffers.joinToString(separator = " ") { "-b $it" }}" +
            "${pid?.let { " --pid=$it" } ?: ""}" +
            "${lastReboot?.let { " -L" } ?: ""}" +
            " ${filters.joinToString(separator = " ") { "${it.tag}:${it.level.name}" }}"
                .trimEnd()
)

enum class LogcatReadMode {
    brief,
    long,
    process,
    raw,
    tag,
    thread,
    threadtime,
    time,

    //Show log buffer event descriptions. This modifier affects event log buffer messages only, and has no effect on the other non-binary buffers. The event descriptions come from the event-log-tags database.
    descriptive,

    //Show each priority level with a different color
    color,

    //Display time in seconds starting from Jan 1, 1970
    epoch,

    //Display time in CPU seconds starting from the last boot
    monotonic,

    //Ensure that any binary logging content is escaped
    printable,

    //If permitted by access controls, display the UID or Android ID of the logged process
    uid,

    //Display the time with precision down to microseconds
    usec,

    //Display time as UTC
    UTC,

    //Add the year to the displayed time
    year,

    //Add the local time zone to the displayed time
    zone
}

enum class LogcatBuffer {
    //View the buffer that contains radio/telephony related messages
    radio,
    //View the interpreted binary system event buffer messages
    events,
    //View the main log buffer (default) does not contain system and crash log messages
    main,
    //View the system log buffer (default)
    system,
    //View the crash log buffer (default)
    crash,
    //View all buffers
    all,
    //Reports main, system, and crash buffers
    default
}

enum class LogcatVerbosityLevel {
    V,
    D,
    I,
    W,
    E,
    F,
    S
}

data class LogcatFilterSpec(val tag: String, val level: LogcatVerbosityLevel)

val SupressAll = LogcatFilterSpec("*", LogcatVerbosityLevel.S)