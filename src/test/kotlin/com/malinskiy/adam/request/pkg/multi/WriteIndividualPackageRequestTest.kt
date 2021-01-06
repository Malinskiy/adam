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
import com.malinskiy.adam.extension.toAndroidChannel
import com.malinskiy.adam.extension.toRequestString
import com.malinskiy.adam.request.Feature
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class WriteIndividualPackageRequestTest {
    @Rule
    @JvmField
    val temp = TemporaryFolder()

    @Test
    fun serialize() {
        val request = stub()
        assertThat(request.serialize().toRequestString())
            .isEqualTo("0042exec:cmd package install-write -S 614 session-id sample-fake.apk -")
    }

    @Test
    fun serializeAbb() {
        val request = WriteIndividualPackageRequest(
            supportedFeatures = listOf(Feature.CMD, Feature.ABB_EXEC),
            file = File(WriteIndividualPackageRequestTest::class.java.getResource("/fixture/sample-fake.apk").file),
            session = "session-id"
        )
        assertThat(request.serialize().toRequestString())
            .isEqualTo("0042abb_exec:package\u0000install-write\u0000-S\u0000614\u0000session-id\u0000sample-fake.apk\u0000-")
    }

    @Test
    fun testRead() {
        val fixture = File(WriteIndividualPackageRequestTest::class.java.getResource("/fixture/sample-fake.apk").file)

        val request = WriteIndividualPackageRequest(
            supportedFeatures = listOf(Feature.CMD),
            file = fixture,
            session = "session-id"
        )
        val response = "Success".toByteArray(Const.DEFAULT_TRANSPORT_ENCODING)
        val actual = temp.newFileWithExtension("apk")
        val byteBufferChannel: ByteWriteChannel = actual.writeChannel()
        runBlocking {
            request.readElement(
                ByteReadChannel(response).toAndroidChannel(),
                byteBufferChannel.toAndroidChannel()
            )
        }

        assertThat(actual.readBytes()).isEqualTo(fixture.readBytes())
    }

    @Test(expected = RequestRejectedException::class)
    fun testReadException() {
        val fixture = File(WriteIndividualPackageRequestTest::class.java.getResource("/fixture/sample-fake.apk").file)

        val request = WriteIndividualPackageRequest(
            supportedFeatures = listOf(Feature.CMD),
            file = fixture,
            session = "session-id"
        )
        val response = "Failure".toByteArray(Const.DEFAULT_TRANSPORT_ENCODING)
        val actual = temp.newFileWithExtension("apk")
        val byteBufferChannel: ByteWriteChannel = actual.writeChannel()
        runBlocking {
            request.readElement(
                ByteReadChannel(response).toAndroidChannel(),
                byteBufferChannel.toAndroidChannel()
            )
        }

        assertThat(actual.readBytes()).isEqualTo(fixture.readBytes())
    }

    private fun stub(): WriteIndividualPackageRequest {
        val request = WriteIndividualPackageRequest(
            supportedFeatures = listOf(Feature.CMD),
            file = File(WriteIndividualPackageRequestTest::class.java.getResource("/fixture/sample-fake.apk").file),
            session = "session-id"
        )
        return request
    }
}
