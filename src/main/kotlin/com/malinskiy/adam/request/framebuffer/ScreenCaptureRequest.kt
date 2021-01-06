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

import com.malinskiy.adam.exception.UnsupportedImageProtocolException
import com.malinskiy.adam.extension.compatRewind
import com.malinskiy.adam.request.ComplexRequest
import com.malinskiy.adam.transport.AndroidReadChannel
import com.malinskiy.adam.transport.AndroidWriteChannel
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ScreenCaptureRequest<T>(private val adapter: ScreenCaptureAdapter<T>) : ComplexRequest<T>() {
    override suspend fun readElement(readChannel: AndroidReadChannel, writeChannel: AndroidWriteChannel): T {
        val protocolBuffer: ByteBuffer = ByteBuffer.allocate(4)
        readChannel.readFully(protocolBuffer)
        protocolBuffer.compatRewind()

        val protocolVersion = protocolBuffer.order(ByteOrder.LITTLE_ENDIAN).int
        val headerSize = when (protocolVersion) {
            1 -> 12 // bpp, size, width, height, 4*(length, offset)
            2 -> 13 // bpp, colorSpace, size, width, height, 4*(length, offset)
            16 -> 3 // compatibility mode: size, width, height. used previously to denote framebuffer depth
            /**
             * See https://android.googlesource.com/platform/packages/modules/adb/+/refs/heads/master/daemon/framebuffer_service.cpp#42
             * for a possible new value for DDMS_RAWIMAGE_VERSION
             */
            else -> throw UnsupportedImageProtocolException(protocolVersion)
        }
        val headerBuffer = ByteBuffer.allocate(headerSize * 4)
        readChannel.readFully(headerBuffer)
        headerBuffer.compatRewind()
        writeChannel.writeFully(ByteArray(1) { 0.toByte() }, 0, 1)

        headerBuffer.order(ByteOrder.LITTLE_ENDIAN)
        headerBuffer.compatRewind()
        return when (protocolVersion) {
            16 -> adapter.process(
                version = protocolVersion,
                bitsPerPixel = 16,
                size = headerBuffer.int,
                width = headerBuffer.int,
                height = headerBuffer.int,
                redOffset = 11,
                redLength = 5,
                greenOffset = 5,
                greenLength = 6,
                blueOffset = 0,
                blueLength = 5,
                alphaOffset = 0,
                alphaLength = 0,
                channel = readChannel
            )
            1 -> adapter.process(
                version = protocolVersion,
                bitsPerPixel = headerBuffer.int,
                size = headerBuffer.int,
                width = headerBuffer.int,
                height = headerBuffer.int,
                redOffset = headerBuffer.int,
                redLength = headerBuffer.int,
                blueOffset = headerBuffer.int,
                blueLength = headerBuffer.int,
                greenOffset = headerBuffer.int,
                greenLength = headerBuffer.int,
                alphaOffset = headerBuffer.int,
                alphaLength = headerBuffer.int,
                channel = readChannel
            )
            2 -> adapter.process(
                version = protocolVersion,
                bitsPerPixel = headerBuffer.int,
                colorSpace = ColorSpace.from(headerBuffer.int),
                size = headerBuffer.int,
                width = headerBuffer.int,
                height = headerBuffer.int,
                redOffset = headerBuffer.int,
                redLength = headerBuffer.int,
                blueOffset = headerBuffer.int,
                blueLength = headerBuffer.int,
                greenOffset = headerBuffer.int,
                greenLength = headerBuffer.int,
                alphaOffset = headerBuffer.int,
                alphaLength = headerBuffer.int,
                channel = readChannel
            )
            else -> throw UnsupportedImageProtocolException(protocolVersion)
        }
    }

    override fun serialize() = createBaseRequest("framebuffer:")
}
