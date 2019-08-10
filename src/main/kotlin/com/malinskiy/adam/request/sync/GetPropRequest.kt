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

package com.malinskiy.adam.request.sync

import com.malinskiy.adam.request.transform.StringResponseTransformer

class GetPropRequest : SyncShellCommandRequest<Map<String, String>>("getprop") {
    private val stringResponseTransformer = StringResponseTransformer()

    override suspend fun process(bytes: ByteArray, offset: Int, limit: Int) = stringResponseTransformer.process(bytes, offset, limit)

    override fun transform(): Map<String, String> = stringResponseTransformer.transform()
        .lines()
        .mapNotNull {
            if (it.isEmpty() || it.startsWith("#")) return@mapNotNull null

            val opens = mutableListOf<Int>()
            val closes = mutableListOf<Int>()

            it.forEachIndexed { i, c ->
                when (c) {
                    '[' -> opens.add(i)
                    ']' -> closes.add(i)
                }
            }

            if (opens.size != 2 || closes.size != 2) return@mapNotNull null

            val key = it.substring(opens[0] + 1, closes[0])
            val value = it.substring(opens[1] + 1, closes[1])

            key to value
        }
        .toMap()
}