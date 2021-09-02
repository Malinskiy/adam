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
import com.malinskiy.adam.request.shell.v1.ShellCommandRequest
import com.malinskiy.adam.request.sync.PullRequest
import com.malinskiy.adam.rule.AdbDeviceRule
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class PullE2ETest {
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
    fun testPull() {
        runBlocking {
            adbRule.adb.execute(ShellCommandRequest("mkdir -p /data/local/tmp/testdir/X"), adbRule.deviceSerial)
            adbRule.adb.execute(ShellCommandRequest("mkdir -p /data/local/tmp/testdir/Y/Z"), adbRule.deviceSerial)
            adbRule.adb.execute(ShellCommandRequest("echo Xcafebabe > /data/local/tmp/testdir/X/testfilex"), adbRule.deviceSerial)
            adbRule.adb.execute(ShellCommandRequest("echo Ycafebabe > /data/local/tmp/testdir/Y/testfiley"), adbRule.deviceSerial)
            adbRule.adb.execute(ShellCommandRequest("echo Zcafebabe > /data/local/tmp/testdir/Y/Z/testfilez"), adbRule.deviceSerial)
            //Testing non-latin filenames
            adbRule.adb.execute(ShellCommandRequest("echo кафебаба > /data/local/tmp/testdir/тестовыйфайл"), adbRule.deviceSerial)

            val dst = temp.newFolder()
            val execute =
                adbRule.adb.execute(PullRequest("/data/local/tmp/testdir", dst, adbRule.supportedFeatures), adbRule.deviceSerial)

            //Should create a subdir since dst already exists
            val testdir = File(dst, "testdir")

            val X = File(testdir, "X")
            val Y = File(testdir, "Y")
            val Z = File(Y, "Z")
            val x = File(X, "testfilex")
            val y = File(Y, "testfiley")
            val z = File(Z, "testfilez")
            val q = File(testdir, "тестовыйфайл")

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

    @Test
    fun testPullFileIntoADestinationFolder() {
        runBlocking {
            adbRule.adb.execute(ShellCommandRequest("mkdir -p /data/local/tmp/testdir/X"), adbRule.deviceSerial)
            adbRule.adb.execute(ShellCommandRequest("echo Xcafebabe > /data/local/tmp/testdir/X/testfilex"), adbRule.deviceSerial)

            val dst = temp.newFolder()
            val execute =
                adbRule.adb.execute(
                    PullRequest("/data/local/tmp/testdir/X/testfilex", dst, adbRule.supportedFeatures),
                    adbRule.deviceSerial
                )

            val x = File(dst, "testfilex")

            assertThat(x).isFile()

            assertThat(x.readText()).isEqualTo("Xcafebabe\n")
        }
    }
}
    
