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
import java.util.*

private val sinceFormatter = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss.SSS")

private val sinceYearFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

sealed class LogcatSinceFormat(val text: String) {
    // It formats with 'MM-dd HH:mm:ss.SSS'
    class DateString(instant: Instant, timezone: String) :
        LogcatSinceFormat("'${sinceFormatter.withZone(TimeZone.getTimeZone(timezone).toZoneId()).format(instant)}'")

    // It formats with 'yyyy-MM-dd HH:mm:ss.SSS'
    class DateStringYear(instant: Instant, timezone: String) :
        LogcatSinceFormat("'${sinceYearFormatter.withZone(TimeZone.getTimeZone(timezone).toZoneId()).format(instant)}'")

    // It formats with 'SSS.0'
    class TimeStamp(instant: Instant) :
        LogcatSinceFormat("${instant.toEpochMilli()}.0")
}
