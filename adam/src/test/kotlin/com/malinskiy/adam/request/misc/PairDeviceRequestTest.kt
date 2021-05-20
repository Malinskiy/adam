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

package com.malinskiy.adam.request.misc

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.malinskiy.adam.extension.toRequestString
import com.malinskiy.adam.server.AndroidDebugBridgeServer
import io.ktor.utils.io.close
import kotlinx.coroutines.runBlocking
import org.junit.Test

class PairDeviceRequestTest {
    @Test
    fun testSerialize() {
        val bytes = PairDeviceRequest("10.0.0.2:39567", "123456").serialize()
        assertThat(bytes.toRequestString()).isEqualTo("001Fhost:pair:123456:10.0.0.2:39567")
    }

    @Test
    fun testResponse() = runBlocking {
        val server = AndroidDebugBridgeServer()

        val client = server.startAndListen { input, output ->
            val cmd = input.receiveCommand()
            assertThat(cmd).isEqualTo("host:pair:123456:10.0.0.2:39567")
            output.respondOkay()

            output.respondStringV1("Successfully paired to 10.0.0.2:39567 [guid=adb-serial-hYG6sO]")
            output.close()
        }

        val output = client.execute(PairDeviceRequest("10.0.0.2:39567", "123456"))
        assertThat(output).isEqualTo("Successfully paired to 10.0.0.2:39567 [guid=adb-serial-hYG6sO]")

        server.dispose()
    }
}
