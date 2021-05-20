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

import com.malinskiy.adam.extension.compatRewind
import com.malinskiy.adam.transport.Socket
import java.awt.image.BufferedImage
import java.nio.ByteBuffer

class BufferedImageScreenCaptureAdapter(
    private var recycledImage: BufferedImage? = null,
    buffer: ByteBuffer? = null,
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
        socket: Socket
    ): BufferedImage {
        val imageBuffer: ByteBuffer = read(socket, size)
        imageBuffer.compatRewind()
        return when (bitsPerPixel) {
            16 -> {
                createOrReuseBufferedImage(colorSpace, width, height, BufferedImage.TYPE_USHORT_565_RGB)
                    .also {
                        val shortArray = ShortArray(imageBuffer.limit() / 2)
                        imageBuffer.asShortBuffer().get(shortArray)
                        it.raster.setDataElements(
                            0, 0, width, height, shortArray
                        )
                    }
            }
            32 -> {
                if (alphaOffset == 24 && alphaLength == 8 &&
                    blueOffset == 16 && blueLength == 8 &&
                    greenOffset == 8 && greenLength == 8 &&
                    redOffset == 0 && redLength == 8
                ) {
                    /**
                     * We can skip processing and directly create a buffer
                     */
                    createOrReuseBufferedImage(colorSpace, width, height, BufferedImage.TYPE_4BYTE_ABGR)
                        .also {
                            it.raster.setDataElements(
                                0, 0, width, height, imageBuffer.array()
                            )
                        }
                } else {
                    createOrReuseBufferedImage(colorSpace, width, height, BufferedImage.TYPE_3BYTE_BGR)
                        .also {
                            for (y in 0 until height) {
                                for (x in 0 until width) {
                                    val bytes: ByteArray = Color.ARGB_INT.toBGR_3BYTE(
                                        Integer.reverseBytes(imageBuffer.int),
                                        redOffset,
                                        redLength,
                                        greenOffset,
                                        greenLength,
                                        blueOffset,
                                        blueLength,
                                        alphaOffset,
                                        alphaLength
                                    )
                                    it.raster.setDataElements(x, y, bytes)
                                }
                            }
                        }
                }
            }
            else -> throw UnsupportedOperationException("BufferedImageScreenCaptureAdapter only works with 16 and 32 bit images")
        }
    }

    private fun createOrReuseBufferedImage(
        colorSpace: ColorSpace?,
        width: Int,
        height: Int,
        type: Int
    ): BufferedImage {
        val bufferedImage = when (val profileName = colorSpace?.getProfileName()) {
            null -> {
                val localRecycledImage = recycledImage
                if (localRecycledImage != null &&
                    localRecycledImage.width == width &&
                    localRecycledImage.height == height &&
                    localRecycledImage.type == type
                ) {
                    localRecycledImage
                } else {
                    BufferedImage(width, height, type)
                }
            }
            else -> {
                val colorModel = colorModelFactory.get(profileName, type)
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
}
