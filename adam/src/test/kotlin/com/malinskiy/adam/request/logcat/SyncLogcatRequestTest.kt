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

class SyncLogcatRequestTest {
    @Test
    fun testSinceNonBlocking() {
        val instant = Instant.parse("2022-07-02T07:41:07Z")

        val actual = testLogcatSinceFormat(LogcatSinceFormat.DateString(instant, "America/New_York"))
        assertThat(actual)
            .isEqualTo("0049shell:logcat -d -t '07-02 03:41:07.000' -v long -b default TAG:E;echo x$?")
    }

    @Test
    fun testSinceYearNonBlocking() {
        val instant = Instant.parse("2022-07-02T07:41:07Z")
        val actual = testLogcatSinceFormat(LogcatSinceFormat.DateStringYear(instant, "America/New_York"))
        assertThat(actual)
            .isEqualTo("004Eshell:logcat -d -t '2022-07-02 03:41:07.000' -v long -b default TAG:E;echo x$?")
    }

    @Test
    fun testSinceTimeStampNonBlocking() {
        val instant = Instant.ofEpochMilli(10)
        val actual = testLogcatSinceFormat(LogcatSinceFormat.TimeStamp(instant))

        assertThat(actual)
            .isEqualTo("0039shell:logcat -d -t 10.0 -v long -b default TAG:E;echo x$?")
    }

    @Test
    fun testDefault() {
        val cmd = SyncLogcatRequest().serialize()

        val actual = String(cmd, Const.DEFAULT_TRANSPORT_ENCODING)
        assertThat(actual)
            .isEqualTo("002Bshell:logcat -d -v long -b default;echo x$?")
    }

    private fun testLogcatSinceFormat(format: LogcatSinceFormat): String {
        val cmd = SyncLogcatRequest(
            since = format,
            filters = listOf(LogcatFilterSpec("TAG", LogcatVerbosityLevel.E)),
        ).serialize()

        return String(cmd, Const.DEFAULT_TRANSPORT_ENCODING)
    }
}
