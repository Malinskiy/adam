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
import com.malinskiy.adam.server.StubSocket
import kotlinx.coroutines.runBlocking
import org.junit.Test

class InstallCommitRequestTest {
    @Test
    fun serialize() {
        val request = stub()
        assertThat(request.serialize().toRequestString())
            .isEqualTo("0031exec:cmd package install-commit parent-session-id")
    }

    @Test
    fun serializeAbb() {
        val request = InstallCommitRequest(
            supportedFeatures = listOf(Feature.CMD, Feature.ABB_EXEC),
            parentSession = "parent-session-id"
        )
        assertThat(request.serialize().toRequestString())
            .isEqualTo("0031abb_exec:package\u0000install-commit\u0000parent-session-id")
    }

    @Test
    fun serializeAbandon() {
        val request = InstallCommitRequest(
            supportedFeatures = listOf(Feature.CMD, Feature.ABB_EXEC),
            parentSession = "parent-session-id",
            abandon = true
        )
        assertThat(request.serialize().toRequestString())
            .isEqualTo("0032abb_exec:package\u0000install-abandon\u0000parent-session-id")
    }

    @Test
    fun testRead() {
        val request = stub()
        val response = "Success [my-session-id]".toByteArray(Const.DEFAULT_TRANSPORT_ENCODING)
        runBlocking {
            StubSocket(response).use { socket ->
                request.readElement(socket)
            }
        }
    }

    @Test(expected = RequestRejectedException::class)
    fun testReadException() {
        val request = stub()
        val response = "Failure".toByteArray(Const.DEFAULT_TRANSPORT_ENCODING)
        runBlocking {
            StubSocket(response).use { socket ->
                request.readElement(socket)
            }
        }
    }

    @Test
    fun testSerializeNoFeatures() {
        assertThat(
            InstallCommitRequest(
                supportedFeatures = listOf(),
                parentSession = "parent-session-id",
                abandon = false
            ).serialize().toRequestString()
        ).isEqualTo("0028exec:pm install-commit parent-session-id")
    }

    private fun stub(): InstallCommitRequest {
        val request = InstallCommitRequest(
            supportedFeatures = listOf(Feature.CMD),
            parentSession = "parent-session-id",
            abandon = false
        )
        return request
    }
}
