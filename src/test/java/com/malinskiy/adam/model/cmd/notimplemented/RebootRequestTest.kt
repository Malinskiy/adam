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

package com.malinskiy.adam.model.cmd.notimplemented

import com.malinskiy.adam.Const
import com.malinskiy.adam.model.cmd.sync.RebootMode
import com.malinskiy.adam.model.cmd.sync.RebootRequest
import org.amshove.kluent.shouldEqual
import org.junit.Test

class RebootRequestTest {
    @Test
    fun testDefault() {
        RebootRequest().serialize().toString(Const.DEFAULT_TRANSPORT_ENCODING) shouldEqual "0007reboot:"
    }

    @Test
    fun testBootloader() {
        RebootRequest(RebootMode.BOOTLOADER).serialize().toString(Const.DEFAULT_TRANSPORT_ENCODING) shouldEqual "0011reboot:bootloader"
    }

    @Test
    fun testRecovery() {
        RebootRequest(RebootMode.RECOVERY).serialize().toString(Const.DEFAULT_TRANSPORT_ENCODING) shouldEqual "000Freboot:recovery"
    }
}