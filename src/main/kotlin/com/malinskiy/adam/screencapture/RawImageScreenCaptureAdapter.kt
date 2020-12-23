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

import com.malinskiy.adam.request.sync.RawImage
import com.malinskiy.adam.transport.AndroidReadChannel

class RawImageScreenCaptureAdapter(buffer: ByteArray? = null) : ScreenCaptureAdapter<RawImage>(buffer = buffer) {
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
    ): RawImage {
        val imageBuffer = read(channel, size)

        return RawImage(
            version = version,
            bitsPerPixel = bitsPerPixel,
            colorSpace = colorSpace,
            size = size,
            width = width,
            height = height,
            redOffset = redOffset,
            redLength = redLength,
            greenOffset = greenOffset,
            greenLength = greenLength,
            blueOffset = blueOffset,
            blueLength = blueLength,
            alphaOffset = alphaOffset,
            alphaLength = alphaLength,
            buffer = imageBuffer
        )
    }
}
