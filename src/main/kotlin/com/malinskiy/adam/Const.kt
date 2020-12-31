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

package com.malinskiy.adam

object Const {
    const val MAX_REMOTE_PATH_LENGTH = 1024
    const val READ_DELAY = 100L
    val DEFAULT_TRANSPORT_ENCODING = Charsets.ISO_8859_1
    val FILENAME_ENCODING = Charsets.UTF_8
    const val DEFAULT_ADB_HOST = "127.0.0.1"
    const val DEFAULT_ADB_PORT = 5037

    const val SERVER_PORT_ENV_VAR = "ANDROID_ADB_SERVER_PORT"
    const val MAX_PACKET_LENGTH = 16384
    const val MAX_FILE_PACKET_LENGTH = 64 * 1024


    object Message {
        val OKAY = byteArrayOf('O'.toByte(), 'K'.toByte(), 'A'.toByte(), 'Y'.toByte())
        val FAIL = byteArrayOf('F'.toByte(), 'A'.toByte(), 'I'.toByte(), 'L'.toByte())

        val DATA = byteArrayOf('D'.toByte(), 'A'.toByte(), 'T'.toByte(), 'A'.toByte())
        val DONE = byteArrayOf('D'.toByte(), 'O'.toByte(), 'N'.toByte(), 'E'.toByte())

        val LSTAT_V1 = byteArrayOf('S'.toByte(), 'T'.toByte(), 'A'.toByte(), 'T'.toByte())
        val LIST_V1 = byteArrayOf('L'.toByte(), 'I'.toByte(), 'S'.toByte(), 'T'.toByte())
        val DENT_V1 = byteArrayOf('D'.toByte(), 'E'.toByte(), 'N'.toByte(), 'T'.toByte())
        val SEND_V1 = byteArrayOf('S'.toByte(), 'E'.toByte(), 'N'.toByte(), 'D'.toByte())
        val RECV_V1 = byteArrayOf('R'.toByte(), 'E'.toByte(), 'C'.toByte(), 'V'.toByte())
    }
}