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

package com.malinskiy.adam.request.sync

import com.malinskiy.adam.exception.UnsupportedImageProtocolException
import com.malinskiy.adam.request.ComplexRequest
import com.malinskiy.adam.transport.AndroidReadChannel
import com.malinskiy.adam.transport.AndroidWriteChannel
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ScreenCaptureRequest : ComplexRequest<RawImage>() {
    override suspend fun readElement(readChannel: AndroidReadChannel, writeChannel: AndroidWriteChannel): RawImage {
        val protocolBuffer: ByteBuffer = ByteBuffer.allocate(4)
        readChannel.readFully(protocolBuffer)
        protocolBuffer.rewind()

        val protocolVersion = protocolBuffer.order(ByteOrder.LITTLE_ENDIAN).int
        val headerSize = when (protocolVersion) {
            1 -> 12 // bpp, size, width, height, 4*(length, offset)
            16 -> 3 // compatibility mode: size, width, height. used previously to denote framebuffer depth
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
