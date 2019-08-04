/*
 * Copyright 2019-2019 Anton Malinskiy
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.android.ddmlib.extension

import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

@Throws(IOException::class)
fun File.wrapToByteBuffer(offset: Long, byteOrder: ByteOrder): ByteBuffer {
    FileInputStream(this).use { dataFile ->
        val fc = dataFile.channel
        val buffer = fc.map(FileChannel.MapMode.READ_ONLY, offset, this.length() - offset)
        buffer.order(byteOrder)
        return buffer
    }
}