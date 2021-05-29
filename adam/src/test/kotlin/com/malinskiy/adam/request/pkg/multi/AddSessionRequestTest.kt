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

package com.malinskiy.adam.request.pkg.multi

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.malinskiy.adam.Const
import com.malinskiy.adam.exception.RequestRejectedException
import com.malinskiy.adam.extension.toRequestString
import com.malinskiy.adam.request.Feature
import com.malinskiy.adam.server.stub.StubSocket
import com.malinskiy.adam.transport.use
import kotlinx.coroutines.runBlocking
import org.junit.Test

class AddSessionRequestTest {
    @Test
    fun serialize() {
        val request = AddSessionRequest(
            childSessions = listOf("child-session-1", "child-session-2"),
            parentSession = "parent-session-1",
            supportedFeatures = listOf(Feature.CMD)
        )
        assertThat(
            request.serialize().toRequestString()
        ).isEqualTo("0055exec:cmd package install-add-session parent-session-1 child-session-1 child-session-2")
    }

    @Test
    fun serializeAbb() {
        val request =
            AddSessionRequest(
                childSessions = listOf("child-session-1", "child-session-2"),
                parentSession = "parent-session-1",
                supportedFeatures = listOf(Feature.CMD, Feature.ABB_EXEC)
            )
        assertThat(
            request.serialize().toRequestString()
        ).isEqualTo("0055abb_exec:package\u0000install-add-session\u0000parent-session-1\u0000child-session-1\u0000child-session-2")
    }

    @Test
    fun testRead() {
        val request =
            AddSessionRequest(
                childSessions = listOf("child-session-1", "child-session-2"),
                parentSession = "parent-session-1",
                supportedFeatures = listOf(Feature.CMD)
            )

        val response = "Success".toByteArray(Const.DEFAULT_TRANSPORT_ENCODING)
        runBlocking {
            StubSocket(response).use { socket ->
                request.readElement(socket)
            }
        }
    }

    @Test(expected = RequestRejectedException::class)
    fun testReadException() {
        val request =
            AddSessionRequest(
                childSessions = listOf("child-session-1", "child-session-2"),
                parentSession = "parent-session-1",
                supportedFeatures = listOf(Feature.CMD)
            )

        val response = "Failure".toByteArray(Const.DEFAULT_TRANSPORT_ENCODING)
        runBlocking {
            StubSocket(response).use { socket ->
                request.readElement(socket)
            }
        }
    }
}
