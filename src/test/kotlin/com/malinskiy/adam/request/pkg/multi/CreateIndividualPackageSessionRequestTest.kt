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
import com.malinskiy.adam.extension.newFileWithExtension
import com.malinskiy.adam.extension.toRequestString
import com.malinskiy.adam.request.Feature
import com.malinskiy.adam.server.StubSocket
import io.ktor.utils.io.core.*
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.text.toByteArray

class CreateIndividualPackageSessionRequestTest {

    @Rule
    @JvmField
    val temp = TemporaryFolder()

    @Test
    fun serialize() {
        val request = stub()
        assertThat(request.serialize().toRequestString())
            .isEqualTo("001Fexec:cmd package install-create")
    }

    @Test
    fun serializeAbb() {
        val pkg = SingleFileInstallationPackage(temp.newFileWithExtension("apk"))
        val request = CreateIndividualPackageSessionRequest(
            supportedFeatures = listOf(Feature.CMD, Feature.ABB_EXEC),
            pkg = pkg,
            pkgList = listOf(pkg),
            reinstall = false
        )
        assertThat(request.serialize().toRequestString())
            .isEqualTo("001Fabb_exec:package\u0000install-create")
    }

    @Test
    fun serializeNoFeatures() {
        val pkg = SingleFileInstallationPackage(temp.newFileWithExtension("apk"))
        val request = CreateIndividualPackageSessionRequest(
            supportedFeatures = listOf(),
            pkg = pkg,
            pkgList = listOf(pkg),
            reinstall = false
        )
        assertThat(request.serialize().toRequestString())
            .isEqualTo("0016exec:pm install-create")
    }

    @Test
    fun serializeCustomized() {
        val pkg = SingleFileInstallationPackage(temp.newFileWithExtension("apk"))
        val request = CreateIndividualPackageSessionRequest(
            supportedFeatures = listOf(Feature.CMD),
            pkg = pkg,
            extraArgs = listOf("-g"),
            pkgList = listOf(pkg),
            reinstall = true
        )
        assertThat(request.serialize().toRequestString())
            .isEqualTo("0027exec:cmd package install-create '-g' -r")
    }

    @Test
    fun serializedApex() {
        val pkg = ApkSplitInstallationPackage(listOf(temp.newFileWithExtension("apex")))
        val request = CreateIndividualPackageSessionRequest(
            supportedFeatures = listOf(Feature.CMD),
            pkg = pkg,
            extraArgs = listOf("-g"),
            pkgList = listOf(pkg),
            reinstall = true
        )
        assertThat(request.serialize().toRequestString())
            .isEqualTo("0037exec:cmd package install-create '-g' -r --staged --apex")
    }

    @Test
    fun testRead() {
        val request = stub()
        val response = "Success [my-session-id]".toByteArray(Const.DEFAULT_TRANSPORT_ENCODING)
        runBlocking {
            StubSocket(response).use { socket ->
                val sessionId = request.readElement(socket)
                assertThat(sessionId).isEqualTo("my-session-id")
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

    @Test(expected = RequestRejectedException::class)
    fun testReadNoSession() {
        val request = stub()
        val response = "Success no session returned".toByteArray(Const.DEFAULT_TRANSPORT_ENCODING)
        runBlocking {
            StubSocket(response).use { socket ->
                request.readElement(socket)
            }
        }
    }

    private fun stub(): CreateIndividualPackageSessionRequest {
        val pkg = SingleFileInstallationPackage(temp.newFileWithExtension("apk"))
        val request = CreateIndividualPackageSessionRequest(
            supportedFeatures = listOf(Feature.CMD),
            pkg = pkg,
            extraArgs = emptyList(),
            pkgList = listOf(pkg),
            reinstall = false
        )
        return request
    }
}
