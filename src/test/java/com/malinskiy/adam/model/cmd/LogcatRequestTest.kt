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

package com.malinskiy.adam.model.cmd

import com.malinskiy.adam.AdbDeviceRule
import com.malinskiy.adam.Const
import com.malinskiy.adam.extension.readAdbString
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.io.ByteChannel
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldEqual
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class LogcatRequestTest {
    @get:Rule
    @JvmField
    val adbRule = AdbDeviceRule()

    @Test
    fun testNonBlocking() {
        val channel = ByteChannel(autoFlush = true)
        val result = GlobalScope.async {
            val buffer = StringBuilder()
            channel.readAdbString { it ->
                buffer.append(it)
            }
            buffer.toString()
        }

        val cmdDeferred = GlobalScope.async {
            adbRule.adb.execute(
                serial = adbRule.deviceSerial,
                request = LogcatRequest(continuous = false),
                response = channel
            )
        }

        runBlocking {
            val response = result.await()
            response.startsWith("--------- beginning of system") shouldBe true
            cmdDeferred.cancelAndJoin()
        }
    }

    @Test
    fun testModeArguments() {
        val cmd = LogcatRequest(
            modes = listOf(LogcatReadMode.long, LogcatReadMode.epoch)
        ).serialize()

        String(cmd, Const.DEFAULT_TRANSPORT_ENCODING) shouldEqual "0028shell:logcat -v long -v epoch -b default"
    }

    @Test
    fun testBuffers() {
        val cmd = LogcatRequest(
            buffers = listOf(LogcatBuffer.crash, LogcatBuffer.radio)
        ).serialize()

        String(cmd, Const.DEFAULT_TRANSPORT_ENCODING) shouldEqual "0026shell:logcat -v long -b crash -b radio"
    }

    @Test
    fun testContinuous() {
        val cmd = LogcatRequest(
            continuous = true
        ).serialize()

        String(cmd, Const.DEFAULT_TRANSPORT_ENCODING) shouldEqual "001Fshell:logcat -v long -b default"
    }

    @Test
    fun testSinceContinuous() {
        val cmd = LogcatRequest(
            continuous = true,
            since = Instant.ofEpochMilli(10)
        ).serialize()

        String(cmd, Const.DEFAULT_TRANSPORT_ENCODING) shouldEqual "0027shell:logcat -T 10.0 -v long -b default"
    }

    @Test
    fun testSinceNonBlocking() {
        val cmd = LogcatRequest(
            continuous = false,
            since = Instant.ofEpochMilli(10)
        ).serialize()

        String(cmd, Const.DEFAULT_TRANSPORT_ENCODING) shouldEqual "002Ashell:logcat -d -t 10.0 -v long -b default"
    }

    @Test
    fun testFilterspec() {
        val cmd = LogcatRequest(
            filters = listOf(SupressAll, LogcatFilterSpec("SOMETAG", LogcatVerbosityLevel.E))
        ).serialize()

        String(cmd, Const.DEFAULT_TRANSPORT_ENCODING) shouldEqual "002Dshell:logcat -v long -b default *:S SOMETAG:E"
    }
}