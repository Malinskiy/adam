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

package com.malinskiy.adam.server.junit5

import com.malinskiy.adam.AndroidDebugBridgeClient
import com.malinskiy.adam.request.misc.GetAdbServerVersionRequest
import com.malinskiy.adam.server.stub.AndroidDebugBridgeServer
import com.malinskiy.adam.server.stub.dsl.Session
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(AdbServerExtension::class)
class AdbServerExtensionTest {
    @AdbClient
    lateinit var client: AndroidDebugBridgeClient

    @AdbServer
    lateinit var server: AndroidDebugBridgeServer

    fun session(block: suspend Session.() -> Unit) {
        server.listen { input, output ->
            val session = Session(input, output)
            block(session)
        }
    }

    @Test
    fun testX() {
        session {
            expectAdbServerVersion()
                .accept()
                .respondAdbServerVersion(41)
        }

        runBlocking {
            val version = client.execute(GetAdbServerVersionRequest())
            assert(version == 41)
        }
    }
}
