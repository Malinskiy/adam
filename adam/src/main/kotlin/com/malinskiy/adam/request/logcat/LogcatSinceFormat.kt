/*
 * Copyright (C) 2022 Anton Malinskiy
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

package com.malinskiy.adam.request.logcat

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val sinceFormatter = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss.SSS")
    .withZone(ZoneId.systemDefault())

private val sinceYearFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    .withZone(ZoneId.systemDefault())

enum class LogcatSinceFormat {
    // It formats with 'MM-dd HH:mm:ss.SSS'
    DATE_STRING,

    // It formats with 'yyyy-MM-dd HH:mm:ss.SSS'
    DATE_STRING_YEAR,

    // It formats with 'SSS.0'
    TIMESTAMP;

    fun format(instant: Instant) = when (this) {
        DATE_STRING -> "'${sinceFormatter.format(instant)}'"
        DATE_STRING_YEAR -> "'${sinceYearFormatter.format(instant)}'"
        TIMESTAMP -> "${instant.toEpochMilli()}.0"
    }
}
