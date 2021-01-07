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

package com.malinskiy.adam.request.abb

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.malinskiy.adam.extension.toAndroidChannel
import com.malinskiy.adam.extension.toRequestString
import com.malinskiy.adam.request.Feature
import io.ktor.util.cio.readChannel
import io.ktor.util.cio.writeChannel
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AbbExecRequestTest {

    @Rule
    @JvmField
    val temp = TemporaryFolder()

    @Test
    fun testSerialize() {
        assertThat(AbbExecRequest(listOf("cmd", "package", "install"), listOf(Feature.ABB_EXEC)).serialize().toRequestString())
            .isEqualTo("001Cabb_exec:cmd\u0000package\u0000install")
    }

    @Test
    fun testValidation() {
        assertThat(AbbExecRequest(listOf(), listOf(Feature.ABB_EXEC)).validate().success)
            .isTrue()
    }

    @Test
    fun testValidationFailure() {
        assertThat(AbbExecRequest(listOf(), supportedFeatures = emptyList()).validate().success)
            .isFalse()
    }

    @Test
    fun testDummy() {
        runBlocking {
            val newFile = temp.newFile()
            val readChannel = newFile.readChannel().toAndroidChannel()
            val writeChannel = newFile.writeChannel().toAndroidChannel()
            assertThat(AbbExecRequest(listOf(), supportedFeatures = emptyList()).readElement(readChannel, writeChannel)).isEqualTo(Unit)
        }
    }
}
