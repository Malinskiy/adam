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

package com.malinskiy.adam.request

import com.malinskiy.adam.Const
import com.malinskiy.adam.request.async.*
import org.amshove.kluent.shouldEqual
import org.junit.Test
import java.time.Instant

class AsyncLogcatRequestTest {
    @Test
    fun testModeArguments() {
        val cmd = LogcatRequestAsync(
            modes = listOf(LogcatReadMode.long, LogcatReadMode.epoch)
        ).serialize()

        String(cmd, Const.DEFAULT_TRANSPORT_ENCODING) shouldEqual "0028shell:logcat -v long -v epoch -b default"
    }

    @Test
    fun testBuffers() {
        val cmd = LogcatRequestAsync(
            buffers = listOf(LogcatBuffer.crash, LogcatBuffer.radio)
        ).serialize()

        String(cmd, Const.DEFAULT_TRANSPORT_ENCODING) shouldEqual "0026shell:logcat -v long -b crash -b radio"
    }

    @Test
    fun testContinuous() {
        val cmd = LogcatRequestAsync().serialize()

        String(cmd, Const.DEFAULT_TRANSPORT_ENCODING) shouldEqual "001Fshell:logcat -v long -b default"
    }

    @Test
    fun testSinceContinuous() {
        val cmd = LogcatRequestAsync(since = Instant.ofEpochMilli(10)).serialize()

        String(cmd, Const.DEFAULT_TRANSPORT_ENCODING) shouldEqual "0027shell:logcat -T 10.0 -v long -b default"
    }

    @Test
    fun testFilterspec() {
        val cmd = LogcatRequestAsync(
            filters = listOf(
                SupressAll,
                LogcatFilterSpec(
                    "SOMETAG",
                    LogcatVerbosityLevel.E
                )
            )
        ).serialize()

        String(cmd, Const.DEFAULT_TRANSPORT_ENCODING) shouldEqual "002Dshell:logcat -v long -b default *:S SOMETAG:E"
    }
}