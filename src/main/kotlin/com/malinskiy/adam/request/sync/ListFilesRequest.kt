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

package com.malinskiy.adam.request.sync

import com.malinskiy.adam.Const


class ListFilesRequest(private val directory: String) : SyncShellCommandRequest<List<AndroidFile>>(
    cmd = "ls -l $directory"
) {
    private val builder = StringBuilder()
    private val lslRegex: Regex = ("^([bcdlsp-][-r][-w][-xsS][-r][-w][-xsS][-r][-w][-xstST])\\s+" + //permissions
            "(?:\\d+\\s+)?" + //nlink
            "(\\S+)\\s+" + //user
            "(\\S+)\\s+" + //group
            "([\\d\\s,]*)\\s+" + //size
            "(\\d{4}-\\d\\d-\\d\\d)\\s+" + //date
            "(\\d\\d:\\d\\d)\\s+" + //time
            "(.*)$").toRegex() //

    override suspend fun process(bytes: ByteArray, offset: Int, limit: Int) {
        val part = String(bytes, 0, limit, Const.DEFAULT_TRANSPORT_ENCODING)
        builder.append(part)
    }

    override fun transform(): List<AndroidFile> {
        return builder.lines()
            .filter { it.isNotBlank() }
            .mapNotNull { lslRegex.find(it) }
            .map { match ->
                val permissions = match.groupValues[1]
                val owner = match.groupValues[2]
                val group = match.groupValues[3]
                val size = match.groupValues[4].toLongOrNull(10) ?: 0
                val date = match.groupValues[5]
                val time = match.groupValues[6]
                var name = match.groupValues[7]

                val type = when (permissions[0]) {
                    '-' -> AndroidFileType.REGULAR_FILE
                    'b' -> AndroidFileType.BLOCK_SPECIAL_FILE
                    'd' -> AndroidFileType.DIRECTORY
                    'l' -> AndroidFileType.SYMBOLIC_LINK
                    'c' -> AndroidFileType.CHARACTER_SPECIAL_FILE
                    's' -> AndroidFileType.SOCKET_LINK
                    'p' -> AndroidFileType.FIFO
                    else -> AndroidFileType.OTHER
                }

                var link: String? = null
                if (type == AndroidFileType.SYMBOLIC_LINK) {
                    val split = name.split("->")
                    if (split.size != 2) {
                        throw RuntimeException("Unable to parse the symbolic file entry $name")
                    }
                    name = split[0].trim()
                    link = split[1].trim()
                }

                AndroidFile(
                    permissions = permissions,
                    owner = owner,
                    group = group,
                    size = size,
                    date = date,
                    time = time,
                    name = name,
                    directory = directory,
                    type = type,
                    link = link
                )
            }
    }
}

enum class AndroidFileType {
    REGULAR_FILE,
    DIRECTORY,
    BLOCK_SPECIAL_FILE,
    CHARACTER_SPECIAL_FILE,
    SYMBOLIC_LINK,
    SOCKET_LINK,
    FIFO,
    OTHER,
}

/**
 * @property permissions full permissions string, e.g. -rw-rw----
 * @property owner file owner, e.g. root
 * @property group file group, e.g. sdcard_rw
 * @property date e.g. 2020-12-01
 * @property time e.g. 22:22
 * @property name the file name without path, e.g. testfile.txt
 * @property directionality file's directory, e.g. /sdcard/
 * @property size file's size, e.g. 1024
 * @property type file's type
 * @property link if the file is a symbolic link, this field is what the link points to
 */
data class AndroidFile(
    val permissions: String,
    val owner: String,
    val group: String,
    val date: String,
    val time: String,
    val name: String,
    val directory: String,
    val size: Long,
    val type: AndroidFileType,
    val link: String? = null
)
