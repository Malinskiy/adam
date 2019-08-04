/*
 * Copyright 2019-2019 Anton Malinskiy
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.android.ddmlib.extension

import java.nio.ByteBuffer

fun ByteBuffer.getString(len: Int): String {
    val data = CharArray(len)
    for (i in 0 until len)
        data[i] = char
    return String(data)
}

fun ByteBuffer.putString(str: String) {
    val len = str.length
    for (i in 0 until len)
        putChar(str[i])
}