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
import com.malinskiy.adam.extension.compatFlip
import com.malinskiy.adam.transport.AdamMaxFilePacketPool
import com.malinskiy.adam.transport.use
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class AsyncFileWriterTest {
    @Rule
    @JvmField
    val temp = TemporaryFolder()

    @Test
    fun testWrite() {
        runBlocking {
            val folder = temp.newFolder()

            val file = File(File(folder, "folder-that-doesnt-exist"), "file-that-doesnt-exist.test")
            AsyncFileWriter(file).use {
                it.start()
                val buffer = AdamMaxFilePacketPool.borrowObject()
                buffer.put("Something interesting!".toByteArray())
                buffer.compatFlip()
                it.write(buffer)
            }

            assertThat(file.readText()).isEqualTo("Something interesting!")
        }
    }
}
