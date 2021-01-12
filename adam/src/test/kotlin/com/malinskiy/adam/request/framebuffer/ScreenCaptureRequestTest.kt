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

package com.malinskiy.adam.request.framebuffer

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.github.romankh3.image.comparison.ImageComparison
import com.github.romankh3.image.comparison.ImageComparisonUtil
import com.github.romankh3.image.comparison.model.ImageComparisonResult
import com.malinskiy.adam.Const
import com.malinskiy.adam.exception.UnsupportedImageProtocolException
import com.malinskiy.adam.extension.newFileWithExtension
import com.malinskiy.adam.server.AndroidDebugBridgeServer
import io.ktor.utils.io.*
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.system.measureTimeMillis


class ScreenCaptureRequestTest {
    @Rule
    @JvmField
    val temp = TemporaryFolder()

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

            val createTempFile = temp.newFileWithExtension("png")
            val actualImage = actual.toBufferedImage()
            ImageIO.write(actualImage, "png", createTempFile)

            val expected = ImageIO.read(File(javaClass.getResource("/fixture/screencap_1.png").toURI()))
            compare(expected, actualImage)

            server.dispose()
        }
    }

    @Test
    fun testProtocol16bit() {
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

                val sample = File(javaClass.getResource("/fixture/screencap_2.bin").toURI()).readBytes()
                output.writeFully(sample, 0, 48)
                assertThat(input.readByte()).isEqualTo(0.toByte())
                output.writeFully(sample, 48, sample.size - 48)
                output.close()
            }

            val adapter = RawImageScreenCaptureAdapter()
            val actual = client.execute(ScreenCaptureRequest(adapter), serial = "serial")
            assertThat(actual.version).isEqualTo(1)
            assertThat(actual.bitsPerPixel).isEqualTo(16)
            assertThat(actual.size).isEqualTo(4147200)
            assertThat(actual.width).isEqualTo(1080)
            assertThat(actual.height).isEqualTo(1920)
            assertThat(actual.redOffset).isEqualTo(11)
            assertThat(actual.redLength).isEqualTo(5)
            assertThat(actual.greenOffset).isEqualTo(5)
            assertThat(actual.greenLength).isEqualTo(6)
            assertThat(actual.blueOffset).isEqualTo(0)
            assertThat(actual.blueLength).isEqualTo(5)
            assertThat(actual.alphaOffset).isEqualTo(0)
            assertThat(actual.alphaLength).isEqualTo(0)
            assertThat(actual.buffer.contentHashCode()).isEqualTo(1300692993)

            val createTempFile = temp.newFileWithExtension("png")
            val actualImage = actual.toBufferedImage()
            ImageIO.write(actualImage, "png", createTempFile)

            val expected = ImageIO.read(File(javaClass.getResource("/fixture/screencap_2.png").toURI()))
            compare(expected, actualImage)

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
        val createTempFile = temp.newFileWithExtension("png")
        ImageIO.write(actual, "png", createTempFile)

        val expected = ImageIO.read(File(javaClass.getResource("/fixture/screencap_1.png").toURI()))

        var comparisonResult = compare(expected, actual!!)
        assertThat(comparisonResult.differencePercent).isEqualTo(0.0f)

        measureTimeMillis {
            actual = client.execute(ScreenCaptureRequest(adapter), serial = "serial")
        }.let { println("Read image with buffer reuse in ${it}ms") }

        val createTempFile2 = temp.newFileWithExtension("png")
        ImageIO.write(actual, "png", createTempFile2)
        comparisonResult = compare(expected, actual!!)
        assertThat(comparisonResult.differencePercent).isEqualTo(0.0f)

        server.dispose()
    }

    /**
     * I only have access to emulator 2.3.3 and it's output in this format is black screen
     * Still worth a test
     */
    @Test
    fun `test with buffered image adapter 16 bit`() = runBlocking {
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

            val sample = File(javaClass.getResource("/fixture/screencap_2.bin").toURI()).readBytes()
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
        val createTempFile = temp.newFileWithExtension("png")
        ImageIO.write(actual, "png", createTempFile)

        val expected = ImageIO.read(File(javaClass.getResource("/fixture/screencap_2.png").toURI()))

        var comparisonResult = compare(expected, actual!!)
        assertThat(comparisonResult.differencePercent).isEqualTo(0.0f)

        measureTimeMillis {
            actual = client.execute(ScreenCaptureRequest(adapter), serial = "serial")
        }.let { println("Read image with buffer reuse in ${it}ms") }

        val createTempFile2 = temp.newFileWithExtension("png")
        ImageIO.write(actual, "png", createTempFile2)
        comparisonResult = compare(expected, actual!!)
        assertThat(comparisonResult.differencePercent).isEqualTo(0.0f)


        server.dispose()
    }

    @Test
    fun `test with buffered image adapter unaligned 32 bit `() = runBlocking {
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

            val sample = File(javaClass.getResource("/fixture/screencap_1_unaligned.bin").toURI()).readBytes()
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
        val createTempFile = temp.newFileWithExtension("png")
        ImageIO.write(actual, "png", createTempFile)

        val expected = ImageIO.read(File(javaClass.getResource("/fixture/screencap_1.png").toURI()))

        var comparisonResult = compare(expected, actual!!)
        assertThat(comparisonResult.differencePercent).isEqualTo(0.0f)

        measureTimeMillis {
            actual = client.execute(ScreenCaptureRequest(adapter), serial = "serial")
        }.let { println("Read image with buffer reuse in ${it}ms") }

        val createTempFile2 = temp.newFileWithExtension("png")
        ImageIO.write(actual, "png", createTempFile2)
        comparisonResult = compare(expected, actual!!)
        assertThat(comparisonResult.differencePercent).isEqualTo(0.0f)

        server.dispose()
    }

    @Test
    fun `test with buffered image adapter with srgb color model`() = runBlocking {
        val server = AndroidDebugBridgeServer()

        val client = server.startAndListen { input, output ->
            val transportCmd = input.receiveCommand()
            assertThat(transportCmd).isEqualTo("host:transport:serial")
            output.respond(Const.Message.OKAY)

            val shellCmd = input.receiveCommand()
            assertThat(shellCmd).isEqualTo("framebuffer:")
            output.respond(Const.Message.OKAY)

            //Extended version
            output.writeIntLittleEndian(2)

            val sample = File(javaClass.getResource("/fixture/screencap_3.bin").toURI()).readBytes()
            output.writeFully(sample, 0, 52)
            assertThat(input.readByte()).isEqualTo(0.toByte())
            output.writeFully(sample, 52, sample.size - 52)
            output.close()
        }

        val adapter = BufferedImageScreenCaptureAdapter()

        var actual: BufferedImage?
        measureTimeMillis {
            actual = client.execute(ScreenCaptureRequest(adapter), serial = "serial")
        }.let { println("Read image in ${it}ms") }
        val createTempFile = temp.newFileWithExtension("png")
        ImageIO.write(actual, "png", createTempFile)

        val expected = ImageIO.read(File(javaClass.getResource("/fixture/screencap_3.png").toURI()))

        var comparisonResult = compare(expected, actual!!)
        assertThat(comparisonResult.differencePercent).isEqualTo(0.0f)

        measureTimeMillis {
            actual = client.execute(ScreenCaptureRequest(adapter), serial = "serial")
        }.let { println("Read image with buffer reuse in ${it}ms") }

        val createTempFile2 = temp.newFileWithExtension("png")
        ImageIO.write(actual, "png", createTempFile2)
        comparisonResult = compare(expected, actual!!)
        assertThat(comparisonResult.differencePercent).isEqualTo(0.0f)

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

    private fun compare(
        expected: BufferedImage,
        actual: BufferedImage
    ): ImageComparisonResult {
        val imageComparison = ImageComparison(expected, actual)
        val comparisonResult = imageComparison.compareImages()
        val comparisonImage = temp.newFileWithExtension("png")
        ImageComparisonUtil.saveImage(comparisonImage, comparisonResult.result)
        return comparisonResult
    }
}
