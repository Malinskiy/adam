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

package com.android.ddmlib.extension

/**
 * Swaps an unsigned int around, and puts the result in an array that can be sent to a device.
 * @receiver The value to swap.
 * @param dest the destination array
 * @param offset the offset in the array where to put the swapped value.
 *      Array length must be at least offset + 4
 */
fun Int.swap32bitsToArray(dest: ByteArray, offset: Int) {
    dest[offset] = (this and 0x000000FF).toByte()
    dest[offset + 1] = (this and 0x0000FF00 shr 8).toByte()
    dest[offset + 2] = (this and 0x00FF0000 shr 16).toByte()
    dest[offset + 3] = (this and -0x1000000 shr 24).toByte()
}

/**
 * Reads a signed 32 bit integer from an array coming from a device.
 * @receiver value the array containing the int
 * @param offset the offset in the array at which the int starts
 * @return the integer read from the array
 */
fun ByteArray.swap32bitFromArray(offset: Int): Int {
    var v = 0
    v = v or (this[offset].toInt() and 0x000000FF)
    v = v or (this[offset + 1].toInt() and 0x000000FF shl 8)
    v = v or (this[offset + 2].toInt() and 0x000000FF shl 16)
    v = v or (this[offset + 3].toInt() and 0x000000FF shl 24)

    return v
}

/**
 * Reads an unsigned 16 bit integer from an array coming from a device,
 * and returns it as an 'int'
 * @receiver the array containing the 16 bit int (2 byte).
 * @param offset the offset in the array at which the int starts
 *      Array length must be at least offset + 2
 * @return the integer read from the array.
 */
fun ByteArray.swapU16bitFromArray(offset: Int): Int {
    var v = 0
    v = v or (this[offset].toInt() and 0x000000FF)
    v = v or (this[offset + 1].toInt() and 0x000000FF shl 8)

    return v
}

/**
 * Reads a signed 64 bit integer from an array coming from a device.
 * @receiver the array containing the int
 * @param offset the offset in the array at which the int starts
 *      Array length must be at least offset + 8
 * @return the integer read from the array
 */
fun ByteArray.swap64bitFromArray(offset: Int): Long {
    var v: Long = 0
    v = v or (this[offset].toLong() and 0x00000000000000FFL)
    v = v or (this[offset + 1].toLong() and 0x00000000000000FFL shl 8)
    v = v or (this[offset + 2].toLong() and 0x00000000000000FFL shl 16)
    v = v or (this[offset + 3].toLong() and 0x00000000000000FFL shl 24)
    v = v or (this[offset + 4].toLong() and 0x00000000000000FFL shl 32)
    v = v or (this[offset + 5].toLong() and 0x00000000000000FFL shl 40)
    v = v or (this[offset + 6].toLong() and 0x00000000000000FFL shl 48)
    v = v or (this[offset + 7].toLong() and 0x00000000000000FFL shl 56)

    return v
}