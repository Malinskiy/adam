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

package com.malinskiy.adam.integration

import assertk.assertThat
import assertk.assertions.isDirectory
import assertk.assertions.isEqualTo
import assertk.assertions.isFile
import assertk.assertions.isTrue
import com.malinskiy.adam.request.shell.v1.ShellCommandRequest
import com.malinskiy.adam.request.sync.PullRequest
import com.malinskiy.adam.request.sync.PushRequest
import com.malinskiy.adam.request.sync.v1.StatFileRequest
import com.malinskiy.adam.rule.AdbDeviceRule
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class PushE2ETest {
    @Rule
    @JvmField
    val adbRule = AdbDeviceRule()

    @Rule
    @JvmField
    val temp = TemporaryFolder()

    @Before
    fun setup() {
        runBlocking {
            adbRule.adb.execute(ShellCommandRequest("rm -r /data/local/tmp/testdir"), adbRule.deviceSerial)
        }
    }

    @After
    fun teardown() {
        runBlocking {
            adbRule.adb.execute(ShellCommandRequest("rm -r /data/local/tmp/testdir"), adbRule.deviceSerial)
        }
    }

    @Test
    fun testPush() {
        val source = temp.newFolder()
        val X = File(source, "X").apply { mkdir() }
        val Y = File(source, "Y").apply { mkdir() }
        val Z = File(Y, "Z").apply { mkdir() }
        val x = File(X, "testfilex").apply { createNewFile(); writeText("Xcafebabe\n") }
        val y = File(Y, "testfiley").apply { createNewFile(); writeText("Ycafebabe\n") }
        val z = File(Z, "testfilez").apply { createNewFile(); writeText("Zcafebabe\n") }
        //Testing non-latin filenames
        val q = File(source, "тестовыйфайл").apply { createNewFile(); writeText("кафебаба\n") }

        runBlocking {
            val execute =
                adbRule.adb.execute(PushRequest(source, "/data/local/tmp/testdir", adbRule.supportedFeatures), adbRule.deviceSerial)

            //Should create a subdir since dst already exists
            assertThat(statFile("/data/local/tmp/testdir").isDirectory()).isTrue()

            assertThat(statFile("/data/local/tmp/testdir/X").isDirectory()).isTrue()
            assertThat(statFile("/data/local/tmp/testdir/Y").isDirectory()).isTrue()
            assertThat(statFile("/data/local/tmp/testdir/Y/Z").isDirectory()).isTrue()

            assertThat(statFile("/data/local/tmp/testdir/X/testfilex").isRegularFile()).isTrue()
            assertThat(statFile("/data/local/tmp/testdir/Y/testfiley").isRegularFile()).isTrue()
            assertThat(statFile("/data/local/tmp/testdir/Y/Z/testfilez").isRegularFile()).isTrue()

            assertThat(readFile("/data/local/tmp/testdir/X/testfilex")).isEqualTo("Xcafebabe\n")
            assertThat(readFile("/data/local/tmp/testdir/Y/testfiley")).isEqualTo("Ycafebabe\n")
            assertThat(readFile("/data/local/tmp/testdir/Y/Z/testfilez")).isEqualTo("Zcafebabe\n")
            assertThat(readFile("/data/local/tmp/testdir/тестовыйфайл")).isEqualTo("кафебаба\n")
        }
    }

    private suspend fun statFile(path: String) = adbRule.adb.execute(StatFileRequest(path), adbRule.deviceSerial)

    private suspend fun readFile(path: String) =
        adbRule.adb.execute(ShellCommandRequest("cat $path"), adbRule.deviceSerial).output

    @Test
    fun testPullDestinationDoesntExist() {
        runBlocking {
            adbRule.adb.execute(ShellCommandRequest("mkdir -p /data/local/tmp/testdir/X"), adbRule.deviceSerial)
            adbRule.adb.execute(ShellCommandRequest("mkdir -p /data/local/tmp/testdir/Y/Z"), adbRule.deviceSerial)
            adbRule.adb.execute(ShellCommandRequest("echo Xcafebabe > /data/local/tmp/testdir/X/testfilex"), adbRule.deviceSerial)
            adbRule.adb.execute(ShellCommandRequest("echo Ycafebabe > /data/local/tmp/testdir/Y/testfiley"), adbRule.deviceSerial)
            adbRule.adb.execute(ShellCommandRequest("echo Zcafebabe > /data/local/tmp/testdir/Y/Z/testfilez"), adbRule.deviceSerial)
            //Testing non-latin filenames
            adbRule.adb.execute(ShellCommandRequest("echo кафебаба > /data/local/tmp/testdir/тестовыйфайл"), adbRule.deviceSerial)

            val dst = temp.newFolder()
            dst.delete()
            val execute =
                adbRule.adb.execute(PullRequest("/data/local/tmp/testdir", dst, adbRule.supportedFeatures), adbRule.deviceSerial)

            val X = File(dst, "X")
            val Y = File(dst, "Y")
            val Z = File(Y, "Z")
            val x = File(X, "testfilex")
            val y = File(Y, "testfiley")
            val z = File(Z, "testfilez")
            val q = File(dst, "тестовыйфайл")

            assertThat(X).isDirectory()
            assertThat(Y).isDirectory()
            assertThat(Z).isDirectory()

            assertThat(x).isFile()
            assertThat(y).isFile()
            assertThat(z).isFile()

            assertThat(x.readText()).isEqualTo("Xcafebabe\n")
            assertThat(y.readText()).isEqualTo("Ycafebabe\n")
            assertThat(z.readText()).isEqualTo("Zcafebabe\n")
            assertThat(q.readText()).isEqualTo("кафебаба\n")
        }
    }
}
