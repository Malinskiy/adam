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

package com.malinskiy.adam.request.logcat

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.malinskiy.adam.Const
import org.junit.Test
import java.time.Instant

class AsyncLogcatRequestTest {
    @Test
    fun testModeArguments() {
        val cmd = ChanneledLogcatRequest(
            modes = listOf(LogcatReadMode.long, LogcatReadMode.epoch)
        ).serialize()

        assertThat(String(cmd, Const.DEFAULT_TRANSPORT_ENCODING))
            .isEqualTo("001Dshell:logcat -v long -v epoch")
    }

    @Test
    fun testBuffers() {
        val cmd = ChanneledLogcatRequest(
            buffers = listOf(LogcatBuffer.crash, LogcatBuffer.radio)
        ).serialize()

        assertThat(String(cmd, Const.DEFAULT_TRANSPORT_ENCODING))
            .isEqualTo("0026shell:logcat -v long -b crash -b radio")
    }

    @Test
    fun testContinuous() {
        val cmd = ChanneledLogcatRequest().serialize()

        assertThat(String(cmd, Const.DEFAULT_TRANSPORT_ENCODING))
            .isEqualTo("0014shell:logcat -v long")
    }

    @Test
    fun testSinceContinuous() {
        val instant = Instant.parse("2022-07-02T07:41:07Z")
        val actual = testLogcatSinceFormat(LogcatSinceFormat.DateString(instant, "America/New_York"))

        assertThat(actual)
            .isEqualTo("002Cshell:logcat -T '07-02 03:41:07.000' -v long")
    }

    @Test
    fun testSinceYearContinuous() {
        val instant = Instant.parse("2022-07-02T07:41:07Z")
        val actual = testLogcatSinceFormat(LogcatSinceFormat.DateStringYear(instant, "America/New_York"))

        assertThat(actual)
            .isEqualTo("0031shell:logcat -T '2022-07-02 03:41:07.000' -v long")
    }

    @Test
    fun testSinceTimestampContinuous() {
        val instant = Instant.ofEpochMilli(10)
        val actual = testLogcatSinceFormat(LogcatSinceFormat.TimeStamp(instant))

        assertThat(actual)
            .isEqualTo("001Cshell:logcat -T 10.0 -v long")
    }

    @Test
    fun testFilterspec() {
        val cmd = ChanneledLogcatRequest(
            filters = listOf(
                SupressAll,
                LogcatFilterSpec(
                    "SOMETAG",
                    LogcatVerbosityLevel.E
                )
            )
        ).serialize()

        assertThat(String(cmd, Const.DEFAULT_TRANSPORT_ENCODING))
            .isEqualTo("0022shell:logcat -v long *:S SOMETAG:E")
    }

    private fun testLogcatSinceFormat(format: LogcatSinceFormat): String {
        val cmd = ChanneledLogcatRequest(since = format).serialize()
        return String(cmd, Const.DEFAULT_TRANSPORT_ENCODING)
    }
}
