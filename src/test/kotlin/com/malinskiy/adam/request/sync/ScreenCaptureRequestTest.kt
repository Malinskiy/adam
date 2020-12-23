/*
 * Copyright (C) 2020 Anton Malinskiy
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
import com.malinskiy.adam.exception.UnsupportedImageProtocolException
import com.malinskiy.adam.screencapture.BufferedImageScreenCaptureAdapter
import com.malinskiy.adam.screencapture.RawImageScreenCaptureAdapter
import com.malinskiy.adam.server.AndroidDebugBridgeServer
import io.ktor.utils.io.*
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.system.measureTimeMillis


class ScreenCaptureRequestTest {
    @Test
    fun testProtocol1() {
        runBlocking {
            val server = AndroidDebugBridgeServer()

            val client = server.startAndListen { input, output ->
                val transportCmd = input.receiveCommand()
                assertThat(transportCmd).isEqualTo("host:transport:serial")
                output.respond(Const.Message.OKAY)

                val shellCmd = input.receiveCommand()
                assertThat(shellCmd).isEqualTo("framebuffer:")
                output.respond(Const.Message.OKAY)

                //Extended version
                output.writeIntLittleEndian(1)

                val sample = File(javaClass.getResource("/fixture/screencap_1.bin").toURI()).readBytes()
                output.writeFully(sample, 0, 48)
                assertThat(input.readByte()).isEqualTo(0.toByte())
                output.writeFully(sample, 48, sample.size - 48)
                output.close()
            }

            val adapter = RawImageScreenCaptureAdapter()
            val actual = client.execute(ScreenCaptureRequest(adapter), serial = "serial")
            assertThat(actual.version).isEqualTo(1)
            assertThat(actual.bitsPerPixel).isEqualTo(32)
            assertThat(actual.size).isEqualTo(8294400)
            assertThat(actual.width).isEqualTo(1080)
            assertThat(actual.height).isEqualTo(1920)
            assertThat(actual.redOffset).isEqualTo(0)
            assertThat(actual.redLength).isEqualTo(8)
            assertThat(actual.greenOffset).isEqualTo(8)
            assertThat(actual.greenLength).isEqualTo(8)
            assertThat(actual.blueOffset).isEqualTo(16)
            assertThat(actual.blueLength).isEqualTo(8)
            assertThat(actual.alphaOffset).isEqualTo(24)
            assertThat(actual.alphaLength).isEqualTo(8)
            assertThat(actual.buffer.contentHashCode()).isEqualTo(-1474724227)

            val createTempFile = createTempFile(suffix = ".png")
            ImageIO.write(actual.toBufferedImage(), "png", createTempFile)
            assertThat(createTempFile.readBytes()).isEqualTo(File(javaClass.getResource("/fixture/screencap_1.png").toURI()).readBytes())

            server.dispose()
        }
    }

    @Test
    fun `test with buffered image adapter`() = runBlocking {
        val server = AndroidDebugBridgeServer()

        val client = server.startAndListen { input, output ->
            val transportCmd = input.receiveCommand()
            assertThat(transportCmd).isEqualTo("host:transport:serial")
            output.respond(Const.Message.OKAY)

            val shellCmd = input.receiveCommand()
            assertThat(shellCmd).isEqualTo("framebuffer:")
            output.respond(Const.Message.OKAY)

            //Extended version
            output.writeIntLittleEndian(1)

            val sample = File(javaClass.getResource("/fixture/screencap_1.bin").toURI()).readBytes()
            output.writeFully(sample, 0, 48)
            assertThat(input.readByte()).isEqualTo(0.toByte())
            output.writeFully(sample, 48, sample.size - 48)
            output.close()
        }

        val adapter = BufferedImageScreenCaptureAdapter()

        var actual: BufferedImage?
        measureTimeMillis {
            actual = client.execute(ScreenCaptureRequest(adapter), serial = "serial")
        }.let { println("Read image in ${it}ms") }
        val createTempFile = createTempFile(suffix = ".png")
        ImageIO.write(actual, "png", createTempFile)
        assertThat(createTempFile.readBytes()).isEqualTo(File(javaClass.getResource("/fixture/screencap_1.png").toURI()).readBytes())

        for (i in 1..10) {
            measureTimeMillis {
                actual = client.execute(ScreenCaptureRequest(adapter), serial = "serial")
            }.let { println("Read image with buffer reuse in ${it}ms") }
        }

        val createTempFile2 = createTempFile(suffix = ".png")
        ImageIO.write(actual, "png", createTempFile2)
        assertThat(createTempFile2.readBytes()).isEqualTo(File(javaClass.getResource("/fixture/screencap_1.png").toURI()).readBytes())

        server.dispose()
    }

    @Test(expected = UnsupportedImageProtocolException::class)
    fun testProtocolUnsupported() {
        runBlocking {
            val server = AndroidDebugBridgeServer()

            val client = server.startAndListen { input, output ->
                val transportCmd = input.receiveCommand()
                assertThat(transportCmd).isEqualTo("host:transport:serial")
                output.respond(Const.Message.OKAY)

                val shellCmd = input.receiveCommand()
                assertThat(shellCmd).isEqualTo("framebuffer:")
                output.respond(Const.Message.OKAY)

                //Extended version
                output.writeIntLittleEndian(99)
                output.close()
            }

            client.execute(ScreenCaptureRequest(RawImageScreenCaptureAdapter()), serial = "serial")
            server.dispose()
        }
    }
}
