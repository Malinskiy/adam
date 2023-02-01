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

package com.malinskiy.adam.integration

import assertk.assertThat
import assertk.assertions.endsWith
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import assertk.assertions.startsWith
import com.malinskiy.adam.Const
import com.malinskiy.adam.request.device.FetchDeviceFeaturesRequest
import com.malinskiy.adam.request.device.ListDevicesRequest
import com.malinskiy.adam.request.framebuffer.RawImageScreenCaptureAdapter
import com.malinskiy.adam.request.framebuffer.ScreenCaptureRequest
import com.malinskiy.adam.request.logcat.ChanneledLogcatRequest
import com.malinskiy.adam.request.mdns.ListMdnsServicesRequest
import com.malinskiy.adam.request.mdns.MdnsCheckRequest
import com.malinskiy.adam.request.misc.FetchHostFeaturesRequest
import com.malinskiy.adam.request.misc.GetAdbServerVersionRequest
import com.malinskiy.adam.request.prop.GetPropRequest
import com.malinskiy.adam.request.prop.GetSinglePropRequest
import com.malinskiy.adam.request.shell.v1.ShellCommandRequest
import com.malinskiy.adam.request.shell.v1.ShellCommandResult
import com.malinskiy.adam.rule.AdbDeviceRule
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO


class E2ETest {
    @Rule
    @JvmField
    val adbRule = AdbDeviceRule()

    @Test
    fun testScreenCapture() {
        runBlocking {
            val image = adbRule.adb.execute(
                ScreenCaptureRequest(RawImageScreenCaptureAdapter()),
                adbRule.deviceSerial
            ).toBufferedImage()

            if (!ImageIO.write(image, "png", File("/tmp/screen.png"))) {
                throw IOException("Failed to find png writer")
            }
        }
    }

    @Test
    fun testMdns() = runBlocking {
        adbRule.adb.execute(ListMdnsServicesRequest()).let { println(it) }
        adbRule.adb.execute(MdnsCheckRequest()).let { println(it) }
    }

    @Test
    fun testEcho() {
        runBlocking {
            val response = adbRule.adb.execute(
                ShellCommandRequest("echo hello"),
                adbRule.deviceSerial
            )
            assertThat(response).isEqualTo(
                ShellCommandResult(
                    "hello${adbRule.lineSeparator}".toByteArray(Const.DEFAULT_TRANSPORT_ENCODING),
                    0
                )
            )
        }
    }

    @Test
    fun testExitCode() {
        runBlocking {
            val response = adbRule.adb.execute(
                ShellCommandRequest("false"),
                adbRule.deviceSerial
            )
            assertThat(response.exitCode).isNotEqualTo(0)
        }
    }

    @Test
    fun testExitCodeParsing() {
        runBlocking {
            val response = adbRule.adb.execute(
                ShellCommandRequest("echo -n 1"),
                adbRule.deviceSerial
            )
            assertThat(response.exitCode).isEqualTo(0)
            assertThat(response.output).isEqualTo("1")
        }
    }

    @Test
    fun testShellLineEnding() {
        runBlocking {
            val lineSeparator = adbRule.adb.execute(
                ShellCommandRequest("echo"),
                adbRule.deviceSerial
            ).output

            val response = adbRule.adb.execute(
                ShellCommandRequest("uname"),
                adbRule.deviceSerial
            )
            assertThat(response.output).endsWith(lineSeparator)
        }
    }

    @Test
    fun testGetProp() {
        runBlocking {
            val props = adbRule.adb.execute(
                GetPropRequest(),
                adbRule.deviceSerial
            )
            assertThat(props["sys.boot_completed"]).isEqualTo("1")
        }
    }

    @Test
    fun testGetPropSingle() {
        runBlocking {
            val response = adbRule.adb.execute(
                GetSinglePropRequest("sys.boot_completed"),
                adbRule.deviceSerial
            )
            assertThat(response).isEqualTo("1${adbRule.lineSeparator}")
        }
    }

    @Test
    fun testNonBlockingLogcat() {
        runBlocking {
            val channel = adbRule.adb.execute(
                serial = adbRule.deviceSerial,
                request = ChanneledLogcatRequest(),
                scope = this
            )

            val line = channel.receive()
            assertThat(line).startsWith("--------- beginning of")
            channel.cancel()
        }
    }

    @Test
    fun testListDevices() {
        runBlocking {
            val list = adbRule.adb.execute(
                ListDevicesRequest(),
                adbRule.deviceSerial
            )

            assertThat(list).hasSize(1)
            assertThat(list[0].serial).isEqualTo(adbRule.deviceSerial)
        }
    }

    @Test
    fun testGetAdbVersion() {
        runBlocking {
            val actual = adbRule.adb.execute(GetAdbServerVersionRequest())
            val expected = ProcessBuilder("adb", "version")
                .start().inputStream.bufferedReader().readText()
            val expectedString = expected.lines().first().substring("Android Debug Bridge version ".length)
            val expectedInt = expectedString.split(".")[2].toInt()
            /**
             * This will change depending on the local version of adb daemon
             * Need to figure out how to test this in a stable fashion
             * Maybe adb service in docker?
             */
            assertThat(actual).isEqualTo(expectedInt)
        }
    }

    @Test
    fun testFetchDeviceFeatures() = runBlocking {
        val features = adbRule.adb.execute(FetchDeviceFeaturesRequest(adbRule.deviceSerial))
        //No exception means it's working, but every emulator has a different feature set
    }

    @Test
    fun testFetchHostFeatures() = runBlocking {
        adbRule.adb.execute(FetchHostFeaturesRequest()).let { println(it) }
        //No exception means it's working
    }
}
