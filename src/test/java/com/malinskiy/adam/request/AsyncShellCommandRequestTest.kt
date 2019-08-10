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

package com.malinskiy.adam.request

import com.malinskiy.adam.Const
import com.malinskiy.adam.request.async.ChanneledShellCommandRequest
import org.amshove.kluent.shouldEqual
import org.junit.Test

class AsyncShellCommandRequestTest {
    @Test
    fun testSimpleCommand() {
        String(ChanneledShellCommandRequest("test").serialize(), Const.DEFAULT_TRANSPORT_ENCODING) shouldEqual "000Ashell:test"
    }
}