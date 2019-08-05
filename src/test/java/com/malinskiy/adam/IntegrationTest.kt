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

package com.malinskiy.adam

import com.malinskiy.adam.model.AndroidDebugBridgeServerFactory
import com.malinskiy.adam.model.cmd.ShellCommandRequest
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.io.ByteChannel
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldEqual
import org.junit.Test

class IntegrationTest {
    @Test
    fun testLs() {
        val adb = AndroidDebugBridgeServerFactory().build()
        val channel = ByteChannel(autoFlush = true)
        val result = GlobalScope.async {
            val buffer = StringBuilder()
            channel.read { it ->
                val line = Const.DEFAULT_TRANSPORT_ENCODING.decode(it).toString()
                buffer.append(line)
            }
            buffer.toString()
        }

        runBlocking {
            adb.execute(
                serial = "emulator-5554",
                request = ShellCommandRequest("echo hello"),
                response = channel
            )
            val response = result.await()
            response shouldEqual "hello\n"
        }
    }
}