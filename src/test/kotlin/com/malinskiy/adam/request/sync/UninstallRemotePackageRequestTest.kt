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

package com.malinskiy.adam.request.sync

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.malinskiy.adam.Const
import com.malinskiy.adam.request.pkg.UninstallRemotePackageRequest
import org.junit.Test

class UninstallRemotePackageRequestTest {
    @Test
    fun testSerialize() {
        val bytes = UninstallRemotePackageRequest("com.example").serialize()

        val actual = String(bytes, Const.DEFAULT_TRANSPORT_ENCODING)
        assertThat(actual)
            .isEqualTo("0027shell:pm uninstall com.example;echo x$?")
    }

    @Test
    fun testSerializeWithRemoveDataFlag() {
        val bytes = UninstallRemotePackageRequest("com.example", true).serialize()

        val actual = String(bytes, Const.DEFAULT_TRANSPORT_ENCODING)
        assertThat(actual)
            .isEqualTo("002Ashell:pm uninstall -k com.example;echo x$?")
    }
}
