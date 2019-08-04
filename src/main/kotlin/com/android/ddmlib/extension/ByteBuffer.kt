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