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

import com.malinskiy.adam.screencapture.Color
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
        return when (bitsPerPixel) {
            16 -> {
                Color.RGB565_2BYTE.toARGB8888_INT(buffer[index], buffer[index + 1])
            }
            32 -> {
                val value = (buffer[index].toInt() and 0x00FF) or
                        (buffer[index + 1].toInt() and 0x00FF shl 8) or
                        (buffer[index + 2].toInt() and 0x00FF shl 16) or
                        (buffer[index + 3].toInt() and 0x00FF shl 24)
                Color.ARGB_INT.toARGB8888_INT(
                    value = value,
                    redOffset = redOffset,
                    redLength = redLength,
                    greenOffset = greenOffset,
                    greenLength = greenLength,
                    blueOffset = blueOffset,
                    blueLength = blueLength,
                    alphaOffset = alphaOffset,
                    alphaLength = alphaLength
                )
            }
            else -> {
                throw UnsupportedOperationException("RawImage.getARGB(int) only works in 16 and 32 bit mode.")
            }
        }
    }

    /**
     * @return TYPE_INT_ARGB buffered image
     */
    fun toBufferedImage(): BufferedImage {
        val bufferedImage = when (val profileName = colorSpace?.getProfileName()) {
            null -> {
                BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
            }
            else -> {
                val colorModel = ColorModelFactory().get(profileName, BufferedImage.TYPE_INT_ARGB)
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

