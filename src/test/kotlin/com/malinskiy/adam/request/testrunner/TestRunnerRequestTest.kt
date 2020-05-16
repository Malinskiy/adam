/*
 * Copyright (C) 2020 Anton Malinskiy
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

package com.malinskiy.adam.request.testrunner

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.malinskiy.adam.Const
import com.malinskiy.adam.extension.toAndroidChannel
import com.malinskiy.adam.server.AndroidDebugBridgeServer
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.close
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.receiveOrNull
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.coroutines.CoroutineContext

class TestRunnerRequestTest : CoroutineScope {
    @Test
    fun testSerialize() {
        val request = TestRunnerRequest(
            testPackage = "com.example.test",
            noWindowAnimations = true,
            instrumentOptions = InstrumentOptions(),
            abi = "x86",
            noHiddenApiChecks = true,
            outputLogPath = "/sdcard/log",
            profilingOutputPath = "/sdcard/profiling",
            runnerClass = "com.example.test.AndroidTestRunner",
            userId = 1000
        )

        assertThat(String(request.serialize(), Const.DEFAULT_TRANSPORT_ENCODING))
            .isEqualTo(
                "00B4shell:am instrument -w -r --no-hidden-api-checks --no-window-animation --user 1000 --abi x86 " +
                        "-p /sdcard/profiling -f /sdcard/log com.example.test/com.example.test.AndroidTestRunner"
            )
    }

    @Test
    fun testDefaultSerialize() {
        val request = TestRunnerRequest(
            testPackage = "com.example.test",
            instrumentOptions = InstrumentOptions()
        )

        assertThat(String(request.serialize(), Const.DEFAULT_TRANSPORT_ENCODING))
            .isEqualTo(
                "0059shell:am instrument -w -r com.example.test/android.support.test.runner.AndroidJUnitRunner"
            )
    }

    @Test
    fun testReturnsProperContent() {
        runBlocking {
            val server = AndroidDebugBridgeServer()
            val client = server.startAndListen { input, output ->
                val transportCmd = input.receiveCommand()
                assertThat(transportCmd).isEqualTo("host:transport:serial")
                output.respond(Const.Message.OKAY)

                val shellCmd = input.receiveCommand()
                assertThat(shellCmd).isEqualTo("shell:am instrument -w -r com.example.test/android.support.test.runner.AndroidJUnitRunner")
                output.respond(Const.Message.OKAY)

                val response = "something-something".toByteArray(Const.DEFAULT_TRANSPORT_ENCODING)
                output.writeFully(response, 0, response.size)
                output.close()
            }

            val channel = client.execute(
                TestRunnerRequest("com.example.test", InstrumentOptions()),
                serial = "serial",
                scope = this@TestRunnerRequestTest
            )
            val builder = StringBuilder()
            while (!channel.isClosedForReceive) {
                val chunk = channel.receiveOrNull() ?: break
                builder.append(chunk)
            }

            assertThat(builder.toString()).isEqualTo("something-something")

            server.dispose()
        }
    }

    @Test
    fun testChannelIsEmpty() {
        val request = TestRunnerRequest("com.example.test", InstrumentOptions())
        val readChannel = ByteChannel(autoFlush = true)
        val writeChannel = ByteChannel(autoFlush = true)
        runBlocking {
            readChannel.close()
            val readElement = request.readElement(
                (readChannel as ByteReadChannel).toAndroidChannel(),
                (writeChannel as ByteWriteChannel).toAndroidChannel()
            )
            assertThat(readElement).isEqualTo("")
        }
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO
}