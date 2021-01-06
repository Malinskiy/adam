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
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.malinskiy.adam.Const
import com.malinskiy.adam.exception.RequestRejectedException
import com.malinskiy.adam.extension.newFileWithExtension
import com.malinskiy.adam.extension.toAndroidChannel
import com.malinskiy.adam.extension.toRequestString
import com.malinskiy.adam.request.Feature
import com.malinskiy.adam.request.ValidationResponse
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.text.toByteArray

class CreateMultiPackageSessionRequestTest {
    @Rule
    @JvmField
    val temp = TemporaryFolder()

    @Test
    fun serialize() {
        val request = stub()
        assertThat(request.serialize().toRequestString())
            .isEqualTo("002Fexec:cmd package install-create --multi-package")
    }

    @Test
    fun serializeAbb() {
        val pkg = SingleFileInstallationPackage(temp.newFileWithExtension("apk"))
        val request = CreateMultiPackageSessionRequest(
            supportedFeatures = listOf(Feature.CMD, Feature.ABB_EXEC),
            extraArgs = emptyList(),
            pkgList = listOf(pkg),
            reinstall = false
        )
        assertThat(request.serialize().toRequestString())
            .isEqualTo("002Fabb_exec:package\u0000install-create\u0000--multi-package")
    }

    @Test
    fun serializeReinstall() {
        val pkg = SingleFileInstallationPackage(temp.newFileWithExtension("apk"))
        val request = CreateMultiPackageSessionRequest(
            supportedFeatures = listOf(Feature.CMD),
            extraArgs = listOf("-g"),
            pkgList = listOf(pkg),
            reinstall = true
        )
        assertThat(request.serialize().toRequestString())
            .isEqualTo("0037exec:cmd package install-create --multi-package '-g' -r")
    }

    @Test
    fun serializeApex() {
        val pkg = SingleFileInstallationPackage(temp.newFileWithExtension("apex"))
        val request = CreateMultiPackageSessionRequest(
            supportedFeatures = listOf(Feature.CMD),
            extraArgs = listOf("-g"),
            pkgList = listOf(pkg),
            reinstall = true
        )
        assertThat(request.serialize().toRequestString())
            .isEqualTo("0040exec:cmd package install-create --multi-package '-g' -r --staged")
    }

    @Test
    fun serializeApkSplit() {
        val pkg = ApkSplitInstallationPackage(listOf(temp.newFileWithExtension("apex"), temp.newFileWithExtension("apex")))
        val request = CreateMultiPackageSessionRequest(
            supportedFeatures = listOf(Feature.CMD),
            extraArgs = listOf("-g"),
            pkgList = listOf(pkg),
            reinstall = true
        )
        assertThat(request.serialize().toRequestString())
            .isEqualTo("0040exec:cmd package install-create --multi-package '-g' -r --staged")
    }

    @Test
    fun testRead() {
        val request = stub()
        val response = "Success [my-session-id]".toByteArray(Const.DEFAULT_TRANSPORT_ENCODING)
        val byteBufferChannel: ByteWriteChannel = ByteChannelSequentialJVM(Buffer.Empty, false)
        runBlocking {
            val sessionId = request.readElement(
                ByteReadChannel(response).toAndroidChannel(),
                byteBufferChannel.toAndroidChannel()
            )
            assertThat(sessionId).isEqualTo("my-session-id")
        }
    }

    @Test(expected = RequestRejectedException::class)
    fun testReadException() {
        val request = stub()
        val response = "Failure".toByteArray(Const.DEFAULT_TRANSPORT_ENCODING)
        val byteBufferChannel: ByteWriteChannel = ByteChannelSequentialJVM(Buffer.Empty, false)
        runBlocking {
            request.readElement(
                ByteReadChannel(response).toAndroidChannel(),
                byteBufferChannel.toAndroidChannel()
            )
        }
    }

    @Test(expected = RequestRejectedException::class)
    fun testReadNoSession() {
        val request = stub()
        val response = "Success no session returned".toByteArray(Const.DEFAULT_TRANSPORT_ENCODING)
        val byteBufferChannel: ByteWriteChannel = ByteChannelSequentialJVM(Buffer.Empty, false)
        runBlocking {
            request.readElement(
                ByteReadChannel(response).toAndroidChannel(),
                byteBufferChannel.toAndroidChannel()
            )
        }
    }

    @Test
    fun testValidation() {
        assertThat(stub().validate()).isEqualTo(ValidationResponse.Success)
    }

    @Test
    fun testValidationFailureFileNotFound() {
        val pkg = SingleFileInstallationPackage(File("/tmp/some-obscure-file-name-that-should-never-be-there.apk"))
        val request = CreateMultiPackageSessionRequest(
            supportedFeatures = listOf(Feature.CMD),
            extraArgs = emptyList(),
            pkgList = listOf(pkg),
            reinstall = false
        )
        assertThat(request.validate().success).isFalse()
    }

    @Test
    fun testValidationFailureNonApk() {
        val pkg = SingleFileInstallationPackage(temp.newFolder("app"))
        val request = CreateMultiPackageSessionRequest(
            supportedFeatures = listOf(Feature.CMD),
            pkgList = listOf(pkg),
            reinstall = false
        )
        assertThat(request.validate().success).isFalse()
    }

    @Test
    fun testValidationFailureFolder() {
        val pkg = SingleFileInstallationPackage(temp.newFolder())
        val request = CreateMultiPackageSessionRequest(
            supportedFeatures = listOf(Feature.CMD),
            extraArgs = emptyList(),
            pkgList = listOf(pkg),
            reinstall = false
        )
        assertThat(request.validate().success).isFalse()
    }

    @Test
    fun testValidationFailureFeatures() {
        val pkg = SingleFileInstallationPackage(temp.newFileWithExtension("apk"))
        var request = CreateMultiPackageSessionRequest(
            supportedFeatures = listOf(),
            extraArgs = emptyList(),
            pkgList = listOf(pkg),
            reinstall = false
        )
        assertThat(request.validate().success).isFalse()

        request = CreateMultiPackageSessionRequest(
            supportedFeatures = listOf(Feature.ABB_EXEC),
            extraArgs = emptyList(),
            pkgList = listOf(pkg),
            reinstall = false
        )
        assertThat(request.validate().success).isTrue()

        request = CreateMultiPackageSessionRequest(
            supportedFeatures = listOf(Feature.CMD),
            extraArgs = emptyList(),
            pkgList = listOf(pkg),
            reinstall = false
        )
        assertThat(request.validate().success).isTrue()
    }

    @Test
    fun testValidationApex() {
        val pkg = SingleFileInstallationPackage(temp.newFileWithExtension("apex"))
        var request = CreateMultiPackageSessionRequest(
            supportedFeatures = listOf(Feature.CMD),
            extraArgs = emptyList(),
            pkgList = listOf(pkg),
            reinstall = false
        )
        assertThat(request.validate().success).isFalse()
    }

    @Test
    fun testValidationApkSplit() {
        val pkg = ApkSplitInstallationPackage(listOf(temp.newFileWithExtension("apk"), temp.newFileWithExtension("apk")))
        var request = CreateMultiPackageSessionRequest(
            supportedFeatures = listOf(Feature.CMD),
            extraArgs = emptyList(),
            pkgList = listOf(pkg),
            reinstall = false
        )
        assertThat(request.validate().success).isTrue()
    }

    @Test
    fun testValidationFailureApkSplit() {
        val pkg = ApkSplitInstallationPackage(listOf(temp.newFileWithExtension("apk"), createTempFile(suffix = ".app")))
        var request = CreateMultiPackageSessionRequest(
            supportedFeatures = listOf(Feature.CMD),
            extraArgs = emptyList(),
            pkgList = listOf(pkg),
            reinstall = false
        )
        assertThat(request.validate().success).isFalse()
    }

    private fun stub(): CreateMultiPackageSessionRequest {
        val pkg = SingleFileInstallationPackage(temp.newFileWithExtension("apk"))
        val request = CreateMultiPackageSessionRequest(
            supportedFeatures = listOf(Feature.CMD),
            extraArgs = emptyList(),
            pkgList = listOf(pkg),
            reinstall = false
        )
        return request
    }
}
