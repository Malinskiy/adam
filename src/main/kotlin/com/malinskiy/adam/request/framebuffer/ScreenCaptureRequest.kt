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
import com.malinskiy.adam.extension.compatLimit
import com.malinskiy.adam.extension.compatRewind
import com.malinskiy.adam.request.ComplexRequest
import com.malinskiy.adam.transport.Socket
import com.malinskiy.adam.transport.withDefaultBuffer
import java.nio.ByteOrder

class ScreenCaptureRequest<T>(private val adapter: ScreenCaptureAdapter<T>) : ComplexRequest<T>() {
    override suspend fun readElement(socket: Socket): T {
        withDefaultBuffer {
            compatLimit(4)
            socket.readFully(this)
            compatRewind()

            val protocolVersion = order(ByteOrder.LITTLE_ENDIAN).int
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
            clear()
            compatLimit(headerSize * 4)
            socket.readFully(this)
            socket.writeFully(ByteArray(1) { 0.toByte() }, 0, 1)

            order(ByteOrder.LITTLE_ENDIAN)
            flip()
            return when (protocolVersion) {
                16 -> adapter.process(
                    version = protocolVersion,
                    bitsPerPixel = 16,
                    size = int,
                    width = int,
                    height = int,
                    redOffset = 11,
                    redLength = 5,
                    greenOffset = 5,
                    greenLength = 6,
                    blueOffset = 0,
                    blueLength = 5,
                    alphaOffset = 0,
                    alphaLength = 0,
                    socket = socket
                )
                1 -> adapter.process(
                    version = protocolVersion,
                    bitsPerPixel = int,
                    size = int,
                    width = int,
                    height = int,
                    redOffset = int,
                    redLength = int,
                    blueOffset = int,
                    blueLength = int,
                    greenOffset = int,
                    greenLength = int,
                    alphaOffset = int,
                    alphaLength = int,
                    socket = socket
                )
                2 -> adapter.process(
                    version = protocolVersion,
                    bitsPerPixel = int,
                    colorSpace = ColorSpace.from(int),
                    size = int,
                    width = int,
                    height = int,
                    redOffset = int,
                    redLength = int,
                    blueOffset = int,
                    blueLength = int,
                    greenOffset = int,
                    greenLength = int,
                    alphaOffset = int,
                    alphaLength = int,
                    socket = socket
                )
                else -> throw UnsupportedImageProtocolException(protocolVersion)
            }
        }
    }

    override fun serialize() = createBaseRequest("framebuffer:")
}
