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
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.cancel
import org.junit.After
import org.junit.Before
import org.junit.Test

class ExecInRequestTest {
    lateinit var channel: ByteChannel

    @Before
    fun prepare() {
        channel = ByteChannel()
    }

    @After
    fun teardown() {
        channel.cancel()
    }

    @Test
    fun testSerialize() {
        assertThat(ExecInRequest("cmd package install", channel).serialize().toRequestString())
            .isEqualTo("0018exec:cmd package install")
    }
}
