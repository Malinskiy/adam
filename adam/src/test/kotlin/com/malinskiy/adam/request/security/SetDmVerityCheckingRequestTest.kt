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

package com.malinskiy.adam.request.security

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.malinskiy.adam.Const
import com.malinskiy.adam.extension.toRequestString
import com.malinskiy.adam.server.AndroidDebugBridgeServer
import io.ktor.utils.io.close
import kotlinx.coroutines.runBlocking
import org.junit.Test

class SetDmVerityCheckingRequestTest {
    @Test
    fun testSerialize() {
        assertThat(SetDmVerityCheckingRequest(false).serialize().toRequestString()).isEqualTo("000Fdisable-verity:")
    }

    @Test
    fun testRead() {
        runBlocking {
            val server = AndroidDebugBridgeServer()

            val client = server.startAndListen { input, output ->
                val transportCmd = input.receiveCommand()
                assertThat(transportCmd).isEqualTo("host:transport:serial")
                output.respond(Const.Message.OKAY)

                val cmd = input.receiveCommand()
                assertThat(cmd).isEqualTo("disable-verity:")
                output.respondOkay()

                output.respondStringRaw("Success")
                output.close()
            }

            val output = client.execute(SetDmVerityCheckingRequest(false), "serial")
            assertThat(output).isEqualTo("Success")

            server.dispose()
        }
    }
}
