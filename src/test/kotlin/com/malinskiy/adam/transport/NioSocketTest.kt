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

package com.malinskiy.adam.transport

import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.net.ConnectException
import java.net.InetSocketAddress

class NioSocketTest {
    @Test(expected = ConnectException::class)
    fun testClosedPort() {
        runBlocking {
            //This will fail obviously in a scenario where 65535 is actually open
            NioSocket(InetSocketAddress("localhost", 65535), 1_000, 1_000).connect()
        }
    }
}