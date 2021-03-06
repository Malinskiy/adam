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

package com.malinskiy.adam.request.mdns

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import com.malinskiy.adam.Const
import com.malinskiy.adam.extension.toRequestString
import com.malinskiy.adam.server.stub.StubSocket
import com.malinskiy.adam.transport.use
import kotlinx.coroutines.runBlocking
import org.junit.Test

class ListMdnsServicesRequestTest {
    @Test
    fun testSerialize() {
        assertThat(ListMdnsServicesRequest().serialize().toRequestString()).isEqualTo("0012host:mdns:services")
    }

    @Test
    fun testReturnsContent() {
        runBlocking {
            val response = "0027adb-serial\t_adb._tcp.\t192.168.1.2:9999\n".toByteArray(Const.DEFAULT_TRANSPORT_ENCODING)

            StubSocket(response).use { socket ->
                val services = ListMdnsServicesRequest().readElement(socket)
                assertThat(services).containsExactly(
                    MdnsService(
                        name = "adb-serial",
                        serviceType = "_adb._tcp.",
                        url = "192.168.1.2:9999"
                    )
                )

                assertThat(services.first().name).isEqualTo("adb-serial")
                assertThat(services.first().serviceType).isEqualTo("_adb._tcp.")
                assertThat(services.first().url).isEqualTo("192.168.1.2:9999")
            }
        }
    }
}
