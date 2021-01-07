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

package com.malinskiy.adam.request.shell.v1

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.fail
import com.malinskiy.adam.Const
import com.malinskiy.adam.server.AndroidDebugBridgeServer
import io.ktor.utils.io.close
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.receiveOrNull
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.coroutines.CoroutineContext

class ChanneledShellCommandRequestTest : CoroutineScope {
    @Test
    fun testReturnsProperContent() {
        runBlocking {
            val server = AndroidDebugBridgeServer()

            val client = server.startAndListen { input, output ->
                val transportCmd = input.receiveCommand()
                assertThat(transportCmd).isEqualTo("host:transport:emulator-5554")
                output.respond(Const.Message.OKAY)

                val shellCmd = input.receiveCommand()
                assertThat(shellCmd).isEqualTo("shell:logcat -v")
                output.respond(Const.Message.OKAY)

                var response = "something-something".toByteArray(Const.DEFAULT_TRANSPORT_ENCODING)
                output.writeFully(response, 0, response.size)
                response = "something2-something2".toByteArray(Const.DEFAULT_TRANSPORT_ENCODING)
                output.writeFully(response, 0, response.size)
                output.close()
            }

            val updates = client.execute(ChanneledShellCommandRequest("logcat -v"), scope = this, serial = "emulator-5554")
            val stringBuffer = StringBuffer()

            while (!updates.isClosedForReceive) {
                stringBuffer.append(updates.receiveOrNull() ?: fail("should receive content"))
            }

            assertThat(stringBuffer.toString()).isEqualTo("something-somethingsomething2-something2")
            server.dispose()
        }
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO
}
