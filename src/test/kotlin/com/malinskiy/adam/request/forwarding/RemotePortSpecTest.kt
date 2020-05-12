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

package com.malinskiy.adam.request.forwarding

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.malinskiy.adam.exception.UnsupportedForwardingSpecException
import org.junit.Test

class RemotePortSpecTest {
    @Test(expected = UnsupportedForwardingSpecException::class)
    fun testUnsupported() {
        RemotePortSpec.parse("wtf")
    }

    @Test
    fun testRemoteAbstractPortSpec() {
        assertThat(RemoteAbstractPortSpec("socket").toSpec()).isEqualTo("localabstract:socket")
    }

    @Test
    fun testRemoteReservedPortSpec() {
        assertThat(RemoteReservedPortSpec("socket").toSpec()).isEqualTo("localreserved:socket")
    }

    @Test
    fun testRemoteDevPortSpec() {
        assertThat(RemoteDevPortSpec("socket").toSpec()).isEqualTo("dev:socket")
    }

    @Test
    fun testJDWPPortSpec() {
        assertThat(JDWPPortSpec(1).toSpec()).isEqualTo("jdwp:1")
    }
}