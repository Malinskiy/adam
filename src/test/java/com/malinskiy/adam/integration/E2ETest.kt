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

import com.malinskiy.adam.request.async.ChanneledLogcatRequest
import com.malinskiy.adam.request.devices.ListDevicesRequest
import com.malinskiy.adam.request.forwarding.*
import com.malinskiy.adam.request.sync.GetPropRequest
import com.malinskiy.adam.request.sync.GetSinglePropRequest
import com.malinskiy.adam.request.sync.ScreenCaptureRequest
import com.malinskiy.adam.request.sync.ShellCommandRequest
import com.malinskiy.adam.rule.AdbDeviceRule
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldEqual
import org.junit.Rule
import org.junit.Test
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO


class E2ETest {
    @get:Rule
    @JvmField
    val adbRule = AdbDeviceRule()

    @Test
    fun testScreenCapture() {
        runBlocking {
            val image = adbRule.adb.execute(
                ScreenCaptureRequest(),
                adbRule.deviceSerial
            )

            val finalImage = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_ARGB)

            var index = 0
            val increment = image.bitsPerPixel shr 3
            for (y in 0 until image.height) {
                for (x in 0 until image.width) {
                    val value = image.getARGB(index)
                    index += increment
                    finalImage.setRGB(x, y, value)
                }
            }

            if (!ImageIO.write(finalImage, "png", File("/tmp/screen.png"))) {
                throw IOException("Failed to find png writer")
            }
        }
    }

    @Test
    fun testEcho() {
        runBlocking {
            val response = adbRule.adb.execute(
                ShellCommandRequest("echo hello"),
                adbRule.deviceSerial
            )
            response shouldEqual "hello"
        }
    }

    @Test
    fun testGetProp() {
        runBlocking {
            val props = adbRule.adb.execute(
                GetPropRequest(),
                adbRule.deviceSerial
            )
            props["sys.boot_completed"]!! shouldEqual "1"
        }
    }

    @Test
    fun testGetPropSingle() {
        runBlocking {
            val response = adbRule.adb.execute(
                GetSinglePropRequest("sys.boot_completed"),
                adbRule.deviceSerial
            )
            response shouldEqual "1"
        }
    }

    @Test
    fun testNonBlockingLogcat() {
        runBlocking {
            val channel = adbRule.adb.execute(
                serial = adbRule.deviceSerial,
                request = ChanneledLogcatRequest(),
                scope = GlobalScope
            )

            val line = channel.receive()
            line.startsWith("--------- beginning of") shouldBe true
            channel.cancel()
        }
    }

    @Test
    fun testPortForward() {
        runBlocking {
            adbRule.adb.execute(
                PortForwardRequest(LocalTcpPortSpec(12042), RemoteTcpPortSpec(12042), serial = adbRule.deviceSerial),
                adbRule.deviceSerial
            )

            val portForwards = adbRule.adb.execute(
                ListPortForwardsRequest(adbRule.deviceSerial),
                adbRule.deviceSerial
            )

            portForwards.size shouldEqual 1
            val rule = portForwards[0]
            rule.serial shouldEqual adbRule.deviceSerial
            (rule.localSpec is LocalTcpPortSpec) shouldEqual true
            (rule.localSpec as LocalTcpPortSpec).port shouldEqual 12042

            (rule.remoteSpec is RemoteTcpPortSpec) shouldEqual true
            (rule.remoteSpec as RemoteTcpPortSpec).port shouldEqual 12042

            adbRule.adb.execute(RemovePortForwardRequest(LocalTcpPortSpec(12042), serial = adbRule.deviceSerial), adbRule.deviceSerial)

            val afterAllForwards = adbRule.adb.execute(
                ListPortForwardsRequest(adbRule.deviceSerial), adbRule.deviceSerial
            )

            afterAllForwards.size shouldEqual 0
        }
    }

    @Test
    fun testPortForwardKillSingle() {
        runBlocking {
            adbRule.adb.execute(
                PortForwardRequest(LocalTcpPortSpec(12042), RemoteTcpPortSpec(12042), serial = adbRule.deviceSerial),
                adbRule.deviceSerial
            )

            adbRule.adb.execute(
                RemovePortForwardRequest(LocalTcpPortSpec(12042), serial = adbRule.deviceSerial),
                adbRule.deviceSerial
            )

            val portForwards = adbRule.adb.execute(
                ListPortForwardsRequest(adbRule.deviceSerial),
                adbRule.deviceSerial
            )

            portForwards.size shouldEqual 0
        }
    }

    @Test
    fun testListDevices() {
        runBlocking {
            val list = adbRule.adb.execute(
                ListDevicesRequest(),
                adbRule.deviceSerial
            )

            list.size shouldEqual 1
            list[0].serial shouldEqual adbRule.deviceSerial
        }
    }
}