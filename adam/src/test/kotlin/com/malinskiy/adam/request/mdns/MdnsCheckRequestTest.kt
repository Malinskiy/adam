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
import assertk.assertions.isEqualTo
import com.malinskiy.adam.Const
import com.malinskiy.adam.server.StubSocket
import com.malinskiy.adam.transport.use
import kotlinx.coroutines.runBlocking
import org.junit.Test

class MdnsCheckRequestTest {
    @Test
    fun testSerialize() {
        assertThat(MdnsCheckRequest().serialize().toRequestString()).isEqualTo("000Fhost:mdns:check")
    }

    @Test
    fun testReturnsContent() {
        runBlocking {
            val response = "001Dmdns daemon version [8787003]".toByteArray(Const.DEFAULT_TRANSPORT_ENCODING)

            StubSocket(response).use { socket ->
                val value = MdnsCheckRequest().readElement(socket)
                assertThat(value).isEqualTo(MdnsStatus(true, "8787003"))
            }
        }
    }

    @Test
    fun testReturnsUnavailable() {
        runBlocking {
            val response = "001Dmdns daemon unavailable87003]".toByteArray(Const.DEFAULT_TRANSPORT_ENCODING)

            StubSocket(response).use { socket ->
                val value = MdnsCheckRequest().readElement(socket)
                assertThat(value).isEqualTo(MdnsStatus(false, null))

            }
        }
    }
}
