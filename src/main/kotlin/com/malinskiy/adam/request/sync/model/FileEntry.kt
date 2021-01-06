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

package com.malinskiy.adam.request.sync.model

import com.malinskiy.adam.Const
import java.time.Instant

sealed class FileEntry {
    abstract val mode: UInt
    abstract val name: String?
    abstract val mtime: Instant

    fun isDirectory() = (mode and Const.FileType.S_IFDIR) == Const.FileType.S_IFDIR
    fun isRegularFile() = (mode and Const.FileType.S_IFREG) == Const.FileType.S_IFREG
    fun isBlockDevice() = (mode and Const.FileType.S_IFBLK) == Const.FileType.S_IFBLK
    fun isCharDevice() = (mode and Const.FileType.S_IFCHR) == Const.FileType.S_IFCHR
    fun isLink() = (mode and Const.FileType.S_IFLNK) == Const.FileType.S_IFLNK

    fun size() = when (this) {
        is FileEntryV1 -> size.toLong().toULong()
        is FileEntryV2 -> size
    }
}

data class FileEntryV1(
    override val name: String? = null,
    override val mode: UInt,
    val size: UInt,
    override val mtime: Instant
) : FileEntry()

data class FileEntryV2(
    val error: UInt,
    val dev: ULong,
    val ino: ULong,
    override val mode: UInt,
    val nlink: UInt,
    val uid: UInt,
    val gid: UInt,
    val size: ULong,
    val atime: Instant,
    override val mtime: Instant,
    val ctime: Instant,
    override val name: String? = null
) : FileEntry()
