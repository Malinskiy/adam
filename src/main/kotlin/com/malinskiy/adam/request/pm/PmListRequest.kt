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

package com.malinskiy.adam.request.pm

import com.malinskiy.adam.Const
import com.malinskiy.adam.request.sync.SyncShellCommandRequest

class PmListRequest(val includePath: Boolean = false) : SyncShellCommandRequest<List<Package>>(
    cmd = StringBuilder().apply {
        append("pm list packages")

        if (includePath) append(" -f")
    }.toString()
) {
    private val builder = StringBuilder()

    override suspend fun process(bytes: ByteArray, offset: Int, limit: Int) {
        val part = String(bytes, 0, limit, Const.DEFAULT_TRANSPORT_ENCODING)
        builder.append(part)
    }

    override fun transform(): List<Package> {
        return builder.lines().mapNotNull {
            if (it.isEmpty()) {
                return@mapNotNull null
            } else {
                val split = it.split(":", "=")
                when (includePath) {
                    true -> {
                        Package(split[2], split[1])
                    }
                    false -> {
                        Package(split[1])
                    }
                }

            }
        }
    }
}

data class Package(
    val name: String,
    val path: String? = null
)