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

import kotlin.math.roundToInt

object Color {
    object RGB565_2BYTE {
        fun toARGB8888_INT(first: Byte, second: Byte): Int {
            var value = first.toInt() and 0x00FF
            value = value or (second.toInt() shl 8 and 0x0FF00)
            // Multiply by 255/31 to convert from 5 bits (31 max) to 8 bits (255)
            val r = (value.ushr(11) and 0x1f) * 255 / 31
            val g = (value.ushr(5) and 0x3f) * 255 / 63
            val b = (value and 0x1f) * 255 / 31
            val a = 0xFF // force alpha to opaque if there's no alpha value in the framebuffer.

            return a shl 24 or (r shl 16) or (g shl 8) or b
        }

        fun toBGR_3BYTE(first: Byte, second: Byte): ByteArray {
            var value = first.toInt() and 0x00FF
            value = value or (second.toInt() shl 8 and 0x0FF00)
            // Multiply by 255/31 to convert from 5 bits (31 max) to 8 bits (255)
            val r = (value.ushr(11) and 0x1f) * 255 / 31
            val g = (value.ushr(5) and 0x3f) * 255 / 63
            val b = (value and 0x1f) * 255 / 31
            return byteArrayOf(b.toByte(), g.toByte(), r.toByte())
        }

    }

    object ARGB_INT {
        fun toARGB8888_INT(
            value: Int,
            redOffset: Int,
            redLength: Int,
            greenOffset: Int,
            greenLength: Int,
            blueOffset: Int,
            blueLength: Int,
            alphaOffset: Int,
            alphaLength: Int
        ): Int {
            val r = value.ushr(redOffset) and getMask(redLength) shl 8 - redLength
            val g = value.ushr(greenOffset) and getMask(greenLength) shl 8 - greenLength
            val b = value.ushr(blueOffset) and getMask(blueLength) shl 8 - blueLength
            val a = value.ushr(alphaOffset) and getMask(alphaLength) shl 8 - alphaLength

            return a shl 24 or (r shl 16) or (g shl 8) or b
        }

        fun toBGR_3BYTE(
            value: Int,
            redOffset: Int,
            redLength: Int,
            greenOffset: Int,
            greenLength: Int,
            blueOffset: Int,
            blueLength: Int,
            alphaOffset: Int,
            alphaLength: Int
        ): ByteArray {
            val r = value.ushr(redOffset) and getMask(redLength) shl 8 - redLength
            val g = value.ushr(greenOffset) and getMask(greenLength) shl 8 - greenLength
            val b = value.ushr(blueOffset) and getMask(blueLength) shl 8 - blueLength
            val a = value.ushr(alphaOffset) and getMask(alphaLength) shl 8 - alphaLength

            return if (a == 255) {
                byteArrayOf(r.toByte(), g.toByte(), b.toByte())
            } else {
                val alphaMultiplier: Double = a / 255.0
                return byteArrayOf(
                    (r * alphaMultiplier).roundToInt().toByte(),
                    (g * alphaMultiplier).roundToInt().toByte(),
                    (b * alphaMultiplier).roundToInt().toByte()
                )
            }
        }
    }

    private inline fun getMask(length: Int): Int {
        return when (length) {
            1 -> 0b1
            2 -> 0b11
            3 -> 0b111
            4 -> 0b1111
            5 -> 0b11111
            6 -> 0b111111
            7 -> 0b1111111
            8 -> 0b11111111
            else -> throw RuntimeException("Unexpected mask length $length")
        }
    }
}