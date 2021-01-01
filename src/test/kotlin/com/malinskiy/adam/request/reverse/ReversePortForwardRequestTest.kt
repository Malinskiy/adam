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

package com.malinskiy.adam.request.reverse

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.malinskiy.adam.Const
import com.malinskiy.adam.request.forwarding.LocalTcpPortSpec
import com.malinskiy.adam.request.forwarding.PortForwardingMode
import com.malinskiy.adam.request.forwarding.RemoteTcpPortSpec
import org.junit.Test

class ReversePortForwardRequestTest {
    @Test
    fun testSerializeDefault() {
        val bytes = ReversePortForwardRequest(RemoteTcpPortSpec(80), LocalTcpPortSpec(80)).serialize()

        assertThat(String(bytes, Const.DEFAULT_TRANSPORT_ENCODING))
            .isEqualTo("001Dreverse:forward:tcp:80;tcp:80")
    }

    @Test
    fun testSerializeNoDefault() {
        val bytes = ReversePortForwardRequest(
            RemoteTcpPortSpec(80),
            LocalTcpPortSpec(80),
            mode = PortForwardingMode.NO_REBIND
        ).serialize()

        assertThat(String(bytes, Const.DEFAULT_TRANSPORT_ENCODING))
            .isEqualTo("0026reverse:forward:norebind:tcp:80;tcp:80")
    }
}