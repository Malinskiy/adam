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

import com.malinskiy.adam.screencapture.ColorModelFactory
import com.malinskiy.adam.screencapture.ColorSpace
import java.awt.image.BufferedImage


data class RawImage(
    val version: Int,
    val bitsPerPixel: Int,
    val colorSpace: ColorSpace? = null,
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
        when (bitsPerPixel) {
            16 -> {
                value = buffer[index].toInt() and 0x00FF
                value = value or (buffer[index + 1].toInt() shl 8 and 0x0FF00)
                // RGB565 to RGB888
                // Multiply by 255/31 to convert from 5 bits (31 max) to 8 bits (255)
                r = (value.ushr(11) and 0x1f) * 255 / 31
                g = (value.ushr(5) and 0x3f) * 255 / 63
                b = (value and 0x1f) * 255 / 31
                a = 0xFF // force alpha to opaque if there's no alpha value in the framebuffer.
            }
            32 -> {
                value = buffer[index].toInt() and 0x00FF
                value = value or (buffer[index + 1].toInt() and 0x00FF shl 8)
                value = value or (buffer[index + 2].toInt() and 0x00FF shl 16)
                value = value or (buffer[index + 3].toInt() and 0x00FF shl 24)
                r = value.ushr(redOffset) and getMask(redLength) shl 8 - redLength
                g = value.ushr(greenOffset) and getMask(greenLength) shl 8 - greenLength
                b = value.ushr(blueOffset) and getMask(blueLength) shl 8 - blueLength
                a = value.ushr(alphaOffset) and getMask(alphaLength) shl 8 - alphaLength
            }
            else -> {
                throw UnsupportedOperationException("RawImage.getARGB(int) only works in 16 and 32 bit mode.")
            }
        }

        return a shl 24 or (r shl 16) or (g shl 8) or b
    }

    private fun getMask(length: Int): Int {
        return (1 shl length) - 1
    }

    fun toBufferedImage(): BufferedImage {
        val bufferedImage = when (val profileName = colorSpace?.getProfileName()) {
            null -> {
                BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
            }
            else -> {
                val colorModel = ColorModelFactory().get(profileName)
                val raster = colorModel.createCompatibleWritableRaster(width, height)
                BufferedImage(colorModel, raster, colorModel.isAlphaPremultiplied, null)
            }
        }

        val bytesPerPixel = bitsPerPixel shr 3
        for (y in 0 until height) {
            for (x in 0 until width) {
                bufferedImage.setRGB(x, y, getARGB((x + y * width) * bytesPerPixel))
            }
        }

        return bufferedImage
    }
}

