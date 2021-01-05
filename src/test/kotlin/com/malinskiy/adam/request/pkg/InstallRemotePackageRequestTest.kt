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

package com.malinskiy.adam.request.pkg

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.malinskiy.adam.Const
import org.junit.Test

class InstallRemotePackageRequestTest {
    @Test
    fun testSerialize() {
        val request = InstallRemotePackageRequest("/data/local/tmp/file.apk", false)
        val value = String(request.serialize(), Const.DEFAULT_TRANSPORT_ENCODING)
        assertThat(value)
            .isEqualTo("0032shell:pm install /data/local/tmp/file.apk;echo x$?")
    }

    @Test
    fun testReinstall() {
        val request = InstallRemotePackageRequest("/data/local/tmp/file.apk", true)
        val value = String(request.serialize(), Const.DEFAULT_TRANSPORT_ENCODING)
        assertThat(value)
            .isEqualTo("0035shell:pm install -r /data/local/tmp/file.apk;echo x$?")
    }

    @Test
    fun testMultipleReinstall() {
        val request = InstallRemotePackageRequest("/data/local/tmp/file.apk", true, listOf("-g -r"))
        val value = String(request.serialize(), Const.DEFAULT_TRANSPORT_ENCODING)
        assertThat(value)
            .isEqualTo("003Bshell:pm install -r -g -r /data/local/tmp/file.apk;echo x$?")
    }
}
