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

package com.malinskiy.adam.io

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.malinskiy.adam.request.transform.StringResponseTransformer
import com.malinskiy.adam.transport.use
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AsyncFileReaderTest {
    @Rule
    @JvmField
    val temp = TemporaryFolder()

    @Test
    fun testExtension() {
        runBlocking {
            val file = temp.newFile().apply {
                writeText("Something interesting!")
            }

            val responseTransformer = StringResponseTransformer()

            AsyncFileReader(file).use {
                it.start()
                it.copyTo(responseTransformer)
            }

            assertThat(responseTransformer.transform()).isEqualTo("Something interesting!")
        }
    }
}
