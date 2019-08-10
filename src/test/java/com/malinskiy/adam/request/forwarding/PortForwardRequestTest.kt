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

package com.malinskiy.adam.request.forwarding

import com.malinskiy.adam.Const
import org.amshove.kluent.shouldEqual
import org.junit.Test

class PortForwardRequestTest {
    @Test
    fun testSerializeDefault() {
        val bytes = PortForwardRequest(LocalTcpPortSpec(80), RemoteTcpPortSpec(80), "emulator-5554").serialize()

        String(bytes, Const.DEFAULT_TRANSPORT_ENCODING) shouldEqual "002Fhost-serial:emulator-5554:forward:tcp:80;tcp:80"
    }

    @Test
    fun testSerializeNoDefault() {
        val bytes = PortForwardRequest(
            LocalTcpPortSpec(80),
            RemoteTcpPortSpec(80),
            "emulator-5554",
            mode = PortForwardingMode.NO_REBIND
        ).serialize()

        String(bytes, Const.DEFAULT_TRANSPORT_ENCODING) shouldEqual "0038host-serial:emulator-5554:forward:norebind:tcp:80;tcp:80"
    }
}