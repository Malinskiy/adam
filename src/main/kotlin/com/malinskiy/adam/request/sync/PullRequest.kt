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

import com.malinskiy.adam.AndroidDebugBridgeClient
import com.malinskiy.adam.annotation.Features
import com.malinskiy.adam.exception.PullFailedException
import com.malinskiy.adam.request.Feature
import com.malinskiy.adam.request.MultiRequest
import com.malinskiy.adam.request.sync.compat.CompatListFileRequest
import com.malinskiy.adam.request.sync.compat.CompatPullFileRequest
import com.malinskiy.adam.request.sync.compat.CompatStatFileRequest
import com.malinskiy.adam.request.sync.model.FileEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.FileTime
import java.time.Instant
import kotlin.coroutines.CoroutineContext

/**
 *
 * Notes:
 * - Doesn't handle file links
 * - Destination doesn't have to exist
 * - If source is a directory and the destination is an existing directory -> a subdirectory will be created
 *
 * @param source can be a file or a directory
 */
@Features(Feature.SENDRECV_V2, Feature.STAT_V2, Feature.LS_V2)
class PullRequest(
    private val source: String,
    private val destination: File,
    private val supportedFeatures: List<Feature>,
    override val coroutineContext: CoroutineContext = Dispatchers.IO
) : MultiRequest<Boolean>(), CoroutineScope {

    override suspend fun execute(androidDebugBridgeClient: AndroidDebugBridgeClient, serial: String?): Boolean =
        with(androidDebugBridgeClient) {
            val remoteFileEntry = execute(CompatStatFileRequest(source, supportedFeatures), serial)
            return when {
                remoteFileEntry.isDirectory() -> {
                    val basename = source.split(ANDROID_FILE_SEPARATOR).last()
                    pullFolder(basename, serial)
                }
                remoteFileEntry.isRegularFile() ||
                        remoteFileEntry.isBlockDevice() ||
                        remoteFileEntry.isCharDevice() -> {
                    pullFile(remoteFileEntry, serial)
                }
                else -> false
            }
        }

    private suspend fun AndroidDebugBridgeClient.pullFile(fileEntry: FileEntry, serial: String?): Boolean {
        val realDestination = if (destination.isDirectory && fileEntry.name != null) {
            File(destination, fileEntry.name)
        } else {
            destination
        }

        return doPullFile(source, realDestination, fileEntry.size().toLong(), serial)
    }

    private suspend fun AndroidDebugBridgeClient.pullFolder(
        folderName: String,
        serial: String?
    ): Boolean {
        if (destination.exists() && !destination.isDirectory) {
            throw PullFailedException("Target $destination is not a directory")
        }

        val realDestination = if (destination.exists()) {
            File(destination, folderName)
        } else {
            destination
        }

        /**
         * Iterate instead of recursion
         */
        val filesToPull = mutableListOf<PullFile>()
        var directoriesToTraverse = listOf(source)

        while (directoriesToTraverse.isNotEmpty()) {
            //We have to use a second collection because we're iterating over directoriesToTraverse
            val currentDepthDirs = mutableListOf<String>()
            for (dir in directoriesToTraverse) {
                listAndPopulate(dir, serial, currentDepthDirs, filesToPull, realDestination)
            }
            directoriesToTraverse = currentDepthDirs
        }

        filesToPull.forEach { file ->
            val fileSuccess = doPullFile(file.remote, file.local, file.size.toLong(), serial)
            if (fileSuccess) {
                Files.setLastModifiedTime(file.local.toPath(), FileTime.from(Instant.ofEpochSecond(file.mtime)))
            } else {
                return false
            }
        }

        return true
    }

    private suspend fun AndroidDebugBridgeClient.listAndPopulate(
        dir: String,
        serial: String?,
        currentDepthDirs: MutableList<String>,
        filesToPull: MutableList<PullFile>,
        realDestination: File
    ) {
        val currentDepthFiles = execute(CompatListFileRequest(dir, supportedFeatures), serial).filterNot { IGNORED_FILES.contains(it.name) }
        for (file in currentDepthFiles) {
            when {
                file.isDirectory() -> currentDepthDirs.add(dir + ANDROID_FILE_SEPARATOR + file.name)
                file.isRegularFile() || file.isCharDevice() || file.isBlockDevice() -> {
                    val remotePath = dir + ANDROID_FILE_SEPARATOR + file.name
                    val remoteRelativePath = remotePath.substringAfter(source)
                    val localRelativePath = remoteRelativePath.replace(ANDROID_FILE_SEPARATOR, File.separator)
                    filesToPull.add(
                        PullFile(
                            local = File(realDestination.absolutePath, localRelativePath),
                            remote = remotePath,
                            mtime = file.mtime.epochSecond,
                            mode = file.mode,
                            size = file.size()
                        )
                    )
                }
            }
        }
    }

    private suspend fun AndroidDebugBridgeClient.doPullFile(
        source: String,
        realDestination: File,
        size: Long,
        serial: String?
    ): Boolean {
        val channel = execute(
            CompatPullFileRequest(source, realDestination, size, supportedFeatures, this@PullRequest, coroutineContext),
            serial
        )
        var progress = 0.0
        for (update in channel) {
            progress = update
        }
        return progress == 1.0
    }

    private data class PullFile(
        val local: File,
        val remote: String,
        val mtime: Long,
        val mode: UInt,
        val size: ULong
    )

    companion object {
        const val ANDROID_FILE_SEPARATOR = "/"
        val IGNORED_FILES = setOf(".", "..")
    }
}
