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

package com.malinskiy.adam.request.sync

import com.malinskiy.adam.request.sync.model.SyncFile

internal class BFFSearch<S, D> {
    suspend fun execute(
        source: S,
        destination: D,
        visitor: suspend (currentDir: S, newDirs: MutableList<S>, newFiles: MutableList<SyncFile>, newTargetDirs: MutableList<D>, destinationRoot: D) -> Unit
    ): Pair<List<SyncFile>, List<D>> {
        /**
         * Iterate instead of recursion
         */
        val fileList = mutableListOf<SyncFile>()
        val dirList = mutableListOf<D>()
        var directoriesToTraverse = listOf(source)

        while (directoriesToTraverse.isNotEmpty()) {
            //We have to use a second collection because we're iterating over directoriesToTraverse
            val currentDepthDirs = mutableListOf<S>()
            for (dir in directoriesToTraverse) {
                visitor(dir, currentDepthDirs, fileList, dirList, destination)
            }
            directoriesToTraverse = currentDepthDirs
        }

        return Pair(fileList, dirList)
    }
}
