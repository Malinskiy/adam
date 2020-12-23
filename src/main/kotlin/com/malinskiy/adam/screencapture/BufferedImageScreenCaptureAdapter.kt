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

package com.malinskiy.adam.screencapture

import com.malinskiy.adam.transport.AndroidReadChannel
import io.ktor.utils.io.bits.*
import java.awt.image.BufferedImage
import java.nio.ByteBuffer

class BufferedImageScreenCaptureAdapter(
    private var recycledImage: BufferedImage? = null,
    buffer: ByteArray? = null,
    colorModelFactory: ColorModelFactory = ColorModelFactory()
) : ScreenCaptureAdapter<BufferedImage>(colorModelFactory = colorModelFactory, buffer = buffer) {
    override suspend fun process(
        version: Int,
        bitsPerPixel: Int,
        size: Int,
        width: Int,
        height: Int,
        redOffset: Int,
        redLength: Int,
        greenOffset: Int,
        greenLength: Int,
        blueOffset: Int,
        blueLength: Int,
        alphaOffset: Int,
        alphaLength: Int,
        colorSpace: ColorSpace?,
        channel: AndroidReadChannel
    ): BufferedImage {
        val bytes = read(channel, size)
        val imageBuffer = ByteBuffer.wrap(bytes)

        val bufferedImage = createOrReuseBufferedImage(colorSpace, width, height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val argb: Int = when (bitsPerPixel) {
                    16 -> {
                        var value = imageBuffer.get().toInt() and 0x00FF
                        value = value or (imageBuffer.get().toInt() shl 8 and 0x0FF00)
                        // RGB565 to RGB888
                        // Multiply by 255/31 to convert from 5 bits (31 max) to 8 bits (255)
                        val r = (value.ushr(11) and 0x1f) * 255 / 31
                        val g = (value.ushr(5) and 0x3f) * 255 / 63
                        val b = (value and 0x1f) * 255 / 31
                        val a = 0xFF // force alpha to opaque if there's no alpha value in the framebuffer.

                        a shl 24 or (r shl 16) or (g shl 8) or b
                    }
                    32 -> {
                        var value: Int = imageBuffer.int.reverseByteOrder()
                        val r = value.ushr(redOffset) and getMask(redLength) shl 8 - redLength
                        val g = value.ushr(greenOffset) and getMask(greenLength) shl 8 - greenLength
                        val b = value.ushr(blueOffset) and getMask(blueLength) shl 8 - blueLength
                        val a = value.ushr(alphaOffset) and getMask(alphaLength) shl 8 - alphaLength

                        a shl 24 or (r shl 16) or (g shl 8) or b
                    }
                    else -> {
                        throw UnsupportedOperationException("BufferedImageScreenCaptureAdapter only works with 16 and 32 bit image mode")
                    }
                }

                bufferedImage.setRGB(x, y, argb or -0x1000000)
            }
        }
        return bufferedImage
    }

    private fun createOrReuseBufferedImage(
        colorSpace: ColorSpace?,
        width: Int,
        height: Int
    ): BufferedImage {
        val bufferedImage = when (val profileName = colorSpace?.getProfileName()) {
            null -> {
                val localRecycledImage = recycledImage
                if (localRecycledImage != null &&
                    localRecycledImage.width == width &&
                    localRecycledImage.height == height &&
                    localRecycledImage.type == BufferedImage.TYPE_INT_ARGB
                ) {
                    localRecycledImage
                } else {
                    BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
                }
            }
            else -> {
                val colorModel = colorModelFactory.get(profileName)
                val localRecycledImage = recycledImage
                if (localRecycledImage != null &&
                    localRecycledImage.colorModel == colorModel &&
                    localRecycledImage.width == width &&
                    localRecycledImage.height == height
                ) {
                    localRecycledImage
                } else {
                    val raster = colorModel.createCompatibleWritableRaster(width, height)
                    BufferedImage(colorModel, raster, colorModel.isAlphaPremultiplied, null)
                }
            }
        }
        recycledImage = bufferedImage
        return bufferedImage
    }

    private inline fun getMask(length: Int): Int {
        return (1 shl length) - 1
    }
}
