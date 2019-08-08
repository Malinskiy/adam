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

import com.malinskiy.adam.extension.readAdbString
import com.malinskiy.adam.model.cmd.async.LogcatRequestAsync
import com.malinskiy.adam.model.cmd.sync.GetPropRequest
import com.malinskiy.adam.model.cmd.sync.GetSinglePropRequest
import com.malinskiy.adam.model.cmd.sync.ScreenCaptureRequest
import com.malinskiy.adam.model.cmd.sync.ShellCommandRequest
import com.malinskiy.adam.rule.AdbDeviceRule
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.io.ByteChannel
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
            val image = adbRule.adb.execute(adbRule.deviceSerial, ScreenCaptureRequest())

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
            val response = adbRule.adb.execute(adbRule.deviceSerial, ShellCommandRequest("echo hello"))
            response shouldEqual "hello"
        }
    }

    @Test
    fun testGetProp() {
        runBlocking {
            val props = adbRule.adb.execute(adbRule.deviceSerial, GetPropRequest())
            props["sys.boot_completed"]!! shouldEqual "1"
        }
    }

    @Test
    fun testGetPropSingle() {
        runBlocking {
            val response = adbRule.adb.execute(adbRule.deviceSerial, GetSinglePropRequest("sys.boot_completed"))
            response shouldEqual "1"
        }
    }

    @Test
    fun testNonBlockingLogcat() {
        val channel = ByteChannel(autoFlush = true)
        val result = GlobalScope.async {
            val buffer = StringBuilder()
            channel.readAdbString { it ->
                buffer.append(it)
            }
            buffer.toString()
        }

        val cmdDeferred = GlobalScope.async {
            adbRule.adb.execute(
                serial = adbRule.deviceSerial,
                request = LogcatRequestAsync(),
                response = channel
            )
        }

        runBlocking {
            val response = result.await()
            response.startsWith("--------- beginning of") shouldBe true
            cmdDeferred.cancelAndJoin()
        }
    }
}