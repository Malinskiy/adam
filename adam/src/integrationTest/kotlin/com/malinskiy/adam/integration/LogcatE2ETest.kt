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

package com.malinskiy.adam.integration

import com.malinskiy.adam.request.logcat.ChanneledLogcatRequest
import com.malinskiy.adam.request.logcat.LogcatSinceFormat
import com.malinskiy.adam.request.logcat.SyncLogcatRequest
import com.malinskiy.adam.request.prop.GetSinglePropRequest
import com.malinskiy.adam.rule.AdbDeviceRule
import kotlinx.coroutines.debug.junit4.CoroutinesTimeout
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern

// These regex comes from https://cs.android.com/android/platform/superproject/+/master:development/tools/bugreport/src/com/android/bugreport/logcat/LogcatParser.java
private val BUFFER_BEGIN_RE = Pattern.compile("--------- beginning of (.*)")
private val LOG_LINE_RE = Pattern.compile(
    "((?:(\\d\\d\\d\\d)-)?(\\d\\d)-(\\d\\d)\\s+(\\d\\d):(\\d\\d):(\\d\\d)\\.(\\d\\d\\d)\\s+(\\d+)\\s+(\\d+)\\s+(.)\\s+)(.*?):\\s(.*)",
    Pattern.MULTILINE
)
private val sinceFormatter = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss.SSS")
    .withZone(ZoneId.systemDefault())

class LogcatE2ETest {
    @Rule
    @JvmField
    val adb = AdbDeviceRule()

    @Rule
    @JvmField
    val timeout = CoroutinesTimeout.seconds(60)

    @Test
    fun testSyncRequest() {
        runBlocking {
            val deviceTimezoneString = adb.adb.execute(GetSinglePropRequest("persist.sys.timezone"), adb.deviceSerial)
                .replace("\n", "")
            val deviceTimezone = TimeZone.getTimeZone(deviceTimezoneString)

            val nowInstant = Instant.now()
            val request = SyncLogcatRequest(LogcatSinceFormat.DateString(nowInstant, deviceTimezoneString), modes = listOf())

            val content = adb.adb.execute(request, adb.deviceSerial)
                .split("\n")
                .mapNotNull { LogLine.of(it, deviceTimezone) }
                .filterIsInstance<LogLine.Log>()
            println(content)

            // Check if only logs after a given time are included
            val zonedInstant = nowInstant.atZone(deviceTimezone.toZoneId()).minusSeconds(5)
            assertThat(content.all { it.instant.isAfter(zonedInstant) }, equalTo(true))
        }
    }

    @Test
    fun testChanneledRequest() {
        runBlocking {
            val deviceTimezoneString = adb.adb.execute(GetSinglePropRequest("persist.sys.timezone"), adb.deviceSerial)
                .replace("\n", "")
            val deviceTimezone = TimeZone.getTimeZone(deviceTimezoneString)

            val nowInstant = Instant.now()
            val request = ChanneledLogcatRequest(LogcatSinceFormat.DateString(nowInstant, deviceTimezoneString), modes = listOf())

            val content = mutableSetOf<LogLine.Log>()
            val channel = adb.adb.execute(request, this, adb.deviceSerial)
            // Receive logcat for max 5 seconds
            for (i in 1..5) {
                content += channel.receive()
                    .split("\n")
                    .mapNotNull { LogLine.of(it, deviceTimezone) }
                    .filterIsInstance<LogLine.Log>()

                delay(100)
            }
            channel.cancel()

            val zonedInstant = nowInstant.atZone(deviceTimezone.toZoneId()).minusSeconds(5)
            assertThat(content.all { it.instant.isAfter(zonedInstant) }, equalTo(true))
        }
    }

    @Suppress("HasPlatformType", "MemberVisibilityCanBePrivate")
    sealed class LogLine(val matcher: Matcher) {
        class BufferLine(rawText: String) : LogLine(BUFFER_BEGIN_RE.matcher(rawText).also { it.find() }) {
            val bufferBegin = matcher.group(1)

            override fun toString() = "[BufferLine] $bufferBegin"
        }

        class Log(rawText: String, val timeZone: TimeZone) : LogLine(LOG_LINE_RE.matcher(rawText).also { it.find() }) {
            val date = Calendar.getInstance(timeZone).apply {
                set(Calendar.MONTH, matcher.group(3).toInt() - 1)
                set(Calendar.DAY_OF_MONTH, matcher.group(4).toInt())
                set(Calendar.HOUR_OF_DAY, matcher.group(5).toInt())
                set(Calendar.MINUTE, matcher.group(6).toInt())
                set(Calendar.SECOND, matcher.group(7).toInt())
                set(Calendar.MILLISECOND, matcher.group(8).toInt())
            }

            val pid = matcher.group(9)
            val tid = matcher.group(10)
            val level = matcher.group(11)[0]
            val tag = matcher.group(12)
            val text = matcher.group(13)

            val instant get() = ZonedDateTime.ofInstant(date.toInstant(), timeZone.toZoneId())

            override fun toString() = "[LogLine] ${sinceFormatter.format(date.toInstant())} $pid $tid $level $tag: $text"

            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as Log

                if (date != other.date) return false
                if (pid != other.pid) return false
                if (tid != other.tid) return false
                if (level != other.level) return false
                if (tag != other.tag) return false
                if (text != other.text) return false

                return true
            }

            override fun hashCode(): Int {
                var result = date?.hashCode() ?: 0
                result = 31 * result + (pid?.hashCode() ?: 0)
                result = 31 * result + (tid?.hashCode() ?: 0)
                result = 31 * result + level.hashCode()
                result = 31 * result + (tag?.hashCode() ?: 0)
                result = 31 * result + (text?.hashCode() ?: 0)
                return result
            }
        }

        companion object {
            fun of(rawText: String, timeZone: TimeZone): LogLine? = when {
                BUFFER_BEGIN_RE.matcher(rawText).matches() -> BufferLine(rawText)
                LOG_LINE_RE.matcher(rawText).matches() -> Log(rawText, timeZone)
                else -> null
            }
        }
    }
}
