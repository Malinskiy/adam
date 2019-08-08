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

package com.malinskiy.adam.model.cmd.sync

import com.malinskiy.adam.exception.UnsupportedImageProtocolException
import com.malinskiy.adam.model.cmd.ComplexRequest
import com.malinskiy.adam.transport.AndroidReadChannel
import com.malinskiy.adam.transport.AndroidWriteChannel
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ScreenCaptureRequest : ComplexRequest<RawImage>() {
    override suspend fun process(readChannel: AndroidReadChannel, writeChannel: AndroidWriteChannel): RawImage {
        val protocolBuffer: ByteBuffer = ByteBuffer.allocate(4)
        readChannel.readFully(protocolBuffer)
        protocolBuffer.rewind()

        val protocolVersion = protocolBuffer.order(ByteOrder.LITTLE_ENDIAN).int
        val headerSize = when (protocolVersion) {
            1 -> 12 // bpp, size, width, height, 4*(length, offset)
            16 -> 3 // compatibility mode: size, width, height
            else -> throw UnsupportedImageProtocolException(protocolVersion)
        }
        val headerBuffer = ByteBuffer.allocate(headerSize * 4)
        readChannel.readFully(headerBuffer)
        headerBuffer.rewind()

        writeChannel.writeFully(ByteArray(1) { 0.toByte() }, 0, 1)

        headerBuffer.order(ByteOrder.LITTLE_ENDIAN)
        headerBuffer.rewind()
        val imageSize = headerBuffer.getInt(4)
        val imageBuffer = ByteBuffer.allocate(imageSize)
        headerBuffer.rewind()
        readChannel.readFully(imageBuffer)

        return RawImage.from(protocolVersion, headerBuffer, imageBuffer)
    }

    override fun serialize() = createBaseRequest("framebuffer:")
}

data class RawImage(
    val version: Int,
    val bitsPerPixel: Int,
    val size: Int,
    val width: Int,
    val height: Int,
    val redOffset: Int,
    val redLength: Int,
    val blueOffset: Int,
    val blueLength: Int,
    val greenOffset: Int,
    val greenLength: Int,
    val alphaOffset: Int,
    val alphaLength: Int,
    val buffer: ByteArray
) {

    fun getARGB(index: Int): Int {
        var value: Int
        val r: Int
        val g: Int
        val b: Int
        val a: Int
        if (bitsPerPixel == 16) {
            value = buffer[index].toInt() and 0x00FF
            value = value or (buffer[index + 1].toInt() shl 8 and 0x0FF00)
            // RGB565 to RGB888
            // Multiply by 255/31 to convert from 5 bits (31 max) to 8 bits (255)
            r = (value.ushr(11) and 0x1f) * 255 / 31
            g = (value.ushr(5) and 0x3f) * 255 / 63
            b = (value and 0x1f) * 255 / 31
            a = 0xFF // force alpha to opaque if there's no alpha value in the framebuffer.
        } else if (bitsPerPixel == 32) {
            value = buffer[index].toInt() and 0x00FF
            value = value or (buffer[index + 1].toInt() and 0x00FF shl 8)
            value = value or (buffer[index + 2].toInt() and 0x00FF shl 16)
            value = value or (buffer[index + 3].toInt() and 0x00FF shl 24)
            r = value.ushr(redOffset) and getMask(redLength) shl 8 - redLength
            g = value.ushr(greenOffset) and getMask(greenLength) shl 8 - greenLength
            b = value.ushr(blueOffset) and getMask(blueLength) shl 8 - blueLength
            a = value.ushr(alphaOffset) and getMask(alphaLength) shl 8 - alphaLength
        } else {
            throw UnsupportedOperationException("RawImage.getARGB(int) only works in 16 and 32 bit mode.")
        }

        return a shl 24 or (r shl 16) or (g shl 8) or b
    }

    private fun getMask(length: Int): Int {
        return (1 shl length) - 1
    }

    companion object {
        private fun ByteBuffer.moveToByteArray(): ByteArray {
            rewind()
            val array = ByteArray(remaining())
            get(array)
            return array
        }

        fun from(version: Int, bytes: ByteBuffer, imageBuffer: ByteBuffer): RawImage {
            return when (version) {
                16 ->
                    RawImage(
                        version = version,
                        bitsPerPixel = bytes.int,
                        size = bytes.int,
                        width = bytes.int,
                        height = bytes.int,
                        redOffset = 11,
                        redLength = 5,
                        greenOffset = 5,
                        greenLength = 6,
                        blueOffset = 0,
                        blueLength = 5,
                        alphaOffset = 0,
                        alphaLength = 0,
                        buffer = imageBuffer.moveToByteArray()
                    )
                1 -> RawImage(
                    version = version,
                    bitsPerPixel = bytes.int,
                    size = bytes.int,
                    width = bytes.int,
                    height = bytes.int,
                    redOffset = bytes.int,
                    redLength = bytes.int,
                    blueOffset = bytes.int,
                    blueLength = bytes.int,
                    greenOffset = bytes.int,
                    greenLength = bytes.int,
                    alphaOffset = bytes.int,
                    alphaLength = bytes.int,
                    buffer = imageBuffer.moveToByteArray()
                )
                else -> throw UnsupportedImageProtocolException(version)
            }

        }
    }
}