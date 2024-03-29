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
    const val DEFAULT_BUFFER_SIZE = 1024
    val DEFAULT_TRANSPORT_ENCODING = Charsets.UTF_8
    const val DEFAULT_ADB_HOST = "127.0.0.1"
    const val DEFAULT_ADB_PORT = 5037

    const val SERVER_PORT_ENV_VAR = "ANDROID_ADB_SERVER_PORT"
    const val MAX_PACKET_LENGTH = 16384
    const val MAX_FILE_PACKET_LENGTH = 64 * 1024

    const val MAX_PROTOBUF_LOGCAT_LENGTH = 10_000
    const val MAX_PROTOBUF_PACKET_LENGTH = 10 * 1024 * 1024L //10Mb
    const val TEST_LOGCAT_METRIC = "com.malinskiy.adam.logcat"

    const val ANDROID_FILE_SEPARATOR = "/"
    val SYNC_IGNORED_FILES = setOf(".", "..")

    object Message {
        val OKAY = byteArrayOf('O'.code.toByte(), 'K'.code.toByte(), 'A'.code.toByte(), 'Y'.code.toByte())
        val FAIL = byteArrayOf('F'.code.toByte(), 'A'.code.toByte(), 'I'.code.toByte(), 'L'.code.toByte())

        val DATA = byteArrayOf('D'.code.toByte(), 'A'.code.toByte(), 'T'.code.toByte(), 'A'.code.toByte())
        val DONE = byteArrayOf('D'.code.toByte(), 'O'.code.toByte(), 'N'.code.toByte(), 'E'.code.toByte())

        val LSTAT_V1 = byteArrayOf('S'.code.toByte(), 'T'.code.toByte(), 'A'.code.toByte(), 'T'.code.toByte())
        val LIST_V1 = byteArrayOf('L'.code.toByte(), 'I'.code.toByte(), 'S'.code.toByte(), 'T'.code.toByte())
        val DENT_V1 = byteArrayOf('D'.code.toByte(), 'E'.code.toByte(), 'N'.code.toByte(), 'T'.code.toByte())
        val SEND_V1 = byteArrayOf('S'.code.toByte(), 'E'.code.toByte(), 'N'.code.toByte(), 'D'.code.toByte())
        val RECV_V1 = byteArrayOf('R'.code.toByte(), 'E'.code.toByte(), 'C'.code.toByte(), 'V'.code.toByte())

        val LIST_V2 = byteArrayOf('L'.code.toByte(), 'I'.code.toByte(), 'S'.code.toByte(), '2'.code.toByte())
        val DENT_V2 = byteArrayOf('D'.code.toByte(), 'N'.code.toByte(), 'T'.code.toByte(), '2'.code.toByte())
        val LSTAT_V2 = byteArrayOf('L'.code.toByte(), 'S'.code.toByte(), 'T'.code.toByte(), '2'.code.toByte())
        val RECV_V2 = byteArrayOf('R'.code.toByte(), 'C'.code.toByte(), 'V'.code.toByte(), '2'.code.toByte())
        val SEND_V2 = byteArrayOf('S'.code.toByte(), 'N'.code.toByte(), 'D'.code.toByte(), '2'.code.toByte())

        val DONEDONE =
            byteArrayOf(
                'D'.code.toByte(),
                'O'.code.toByte(),
                'N'.code.toByte(),
                'E'.code.toByte(),
                'D'.code.toByte(),
                'O'.code.toByte(),
                'N'.code.toByte(),
                'E'.code.toByte()
            )
        val FAILFAIL =
            byteArrayOf(
                'F'.code.toByte(),
                'A'.code.toByte(),
                'I'.code.toByte(),
                'L'.code.toByte(),
                'F'.code.toByte(),
                'A'.code.toByte(),
                'I'.code.toByte(),
                'L'.code.toByte()
            )
    }

    object FileType {
        val S_IFMT = "170000".toUInt(8)
        val S_IFIFO = "10000".toUInt(8)
        val S_IFCHR = "20000".toUInt(8)
        val S_IFDIR = "40000".toUInt(8)
        val S_IFBLK = "60000".toUInt(8)
        val S_IFREG = "100000".toUInt(8)
        val S_IFLNK = "120000".toUInt(8)
        val S_IFSOCK = "140000".toUInt(8)
    }
}
