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

package com.malinskiy.adam.request.misc

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.malinskiy.adam.Const
import kotlinx.coroutines.runBlocking
import org.junit.Test

class RebootRequestTest {
    @Test
    fun testDefault() {
        val actual = RebootRequest().serialize().toString(Const.DEFAULT_TRANSPORT_ENCODING)
        assertThat(actual)
            .isEqualTo("0007reboot:")
    }

    @Test
    fun testBootloader() {
        val actual = RebootRequest(RebootMode.BOOTLOADER).serialize().toString(Const.DEFAULT_TRANSPORT_ENCODING)
        assertThat(actual)
            .isEqualTo("0011reboot:bootloader")
    }

    @Test
    fun testRecovery() {
        val actual = RebootRequest(RebootMode.RECOVERY).serialize().toString(Const.DEFAULT_TRANSPORT_ENCODING)
        assertThat(actual)
            .isEqualTo("000Freboot:recovery")
    }

    @Test
    fun testSideload() {
        val actual = RebootRequest(RebootMode.SIDELOAD).serialize().toString(Const.DEFAULT_TRANSPORT_ENCODING)
        assertThat(actual)
            .isEqualTo("000Freboot:sideload")
    }

    @Test
    fun testSideloadAutoReboot() {
        val actual = RebootRequest(RebootMode.SIDELOAD_AUTO_REBOOT).serialize().toString(Const.DEFAULT_TRANSPORT_ENCODING)
        assertThat(actual)
            .isEqualTo("001Breboot:sideload-auto-reboot")
    }

    @Test
    fun testDummy() {
        runBlocking {
            assertThat(RebootRequest(RebootMode.RECOVERY).process(ByteArray(1), 0, 1)).isEqualTo(Unit)
            assertThat(RebootRequest(RebootMode.RECOVERY).transform()).isEqualTo(Unit)
        }
    }
}
