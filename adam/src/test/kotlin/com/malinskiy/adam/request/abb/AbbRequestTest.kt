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
import com.malinskiy.adam.Const
import com.malinskiy.adam.request.Feature
import org.junit.Test

class AbbRequestTest {
    /**
     * It's a bit tricky to check for '\0'
     */
    @Test
    fun testSerialize() {
        val array = AbbRequest(listOf("package", "install"), listOf(Feature.ABB)).serialize()

        assertThat(String(array, 0, 15, Const.DEFAULT_TRANSPORT_ENCODING)).isEqualTo("0013abb:package")
        assertThat(array[15].toInt().toChar()).isEqualTo(AbbExecRequest.DELIMITER)
        assertThat(String(array, 16, 7, Const.DEFAULT_TRANSPORT_ENCODING)).isEqualTo("install")
    }

    @Test
    fun testValidation() {
        assertThat(AbbRequest(listOf(), listOf(Feature.ABB)).validate().success)
            .isTrue()
    }

    @Test
    fun testValidationFailure() {
        assertThat(AbbRequest(listOf(), supportedFeatures = emptyList()).validate().success)
            .isFalse()
    }
}
