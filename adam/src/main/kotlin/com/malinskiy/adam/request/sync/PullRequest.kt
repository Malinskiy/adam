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
import com.malinskiy.adam.Const
import com.malinskiy.adam.annotation.Features
import com.malinskiy.adam.exception.PullFailedException
import com.malinskiy.adam.exception.PushFailedException
import com.malinskiy.adam.request.Feature
import com.malinskiy.adam.request.MultiRequest
import com.malinskiy.adam.request.sync.compat.CompatListFileRequest
import com.malinskiy.adam.request.sync.compat.CompatPullFileRequest
import com.malinskiy.adam.request.sync.compat.CompatStatFileRequest
import com.malinskiy.adam.request.sync.model.SyncFile
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
 * All features are optional
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

    /**
     * @return true if successful, false if not. false can be a partial success: some files might be pulled
     */
    override suspend fun execute(androidDebugBridgeClient: AndroidDebugBridgeClient, serial: String?): Boolean =
        with(androidDebugBridgeClient) {
            val remoteFileEntry = execute(CompatStatFileRequest(source, supportedFeatures), serial)

            when {
                destination.isFile -> {
                    when {
                        remoteFileEntry.isDirectory() -> {
                            throw PullFailedException("Can't pull folder $source: target $destination is a file")
                        }

                        remoteFileEntry.isRegularFile() ||
                                remoteFileEntry.isBlockDevice() ||
                                remoteFileEntry.isCharDevice() -> {
                            doPullFile(source, destination, remoteFileEntry.size().toLong(), serial)
                        }

                        remoteFileEntry.exists() -> {
                            throw PushFailedException(
                                "Source $source exists and is not a directory or a file: mode=${
                                    remoteFileEntry.mode.toString(
                                        8
                                    )
                                }"
                            )
                        }

                        !remoteFileEntry.exists() -> {
                            throw PushFailedException("Source $source doesn't exist")
                        }
                        //We exhausted all conditions above. This is just to make the compiler happy
                        else -> false
                    }
                }

                destination.isDirectory -> {
                    when {
                        remoteFileEntry.isDirectory() -> {
                            val basename = source.removeSuffix(Const.ANDROID_FILE_SEPARATOR)
                                .split(Const.ANDROID_FILE_SEPARATOR)
                                .last()
                            pullFolder(File(destination, basename), serial)
                        }

                        remoteFileEntry.isRegularFile() ||
                                remoteFileEntry.isBlockDevice() ||
                                remoteFileEntry.isCharDevice() -> {
                            val name = source.substringAfterLast(Const.ANDROID_FILE_SEPARATOR)
                            doPullFile(source, File(destination, name), remoteFileEntry.size().toLong(), serial)
                        }

                        remoteFileEntry.exists() -> {
                            throw PushFailedException(
                                "Source $source exists and is not a directory or a file: mode=${
                                    remoteFileEntry.mode.toString(
                                        8
                                    )
                                }"
                            )
                        }

                        !remoteFileEntry.exists() -> {
                            throw PushFailedException("Source $source doesn't exist")
                        }
                        //We exhausted all conditions above. This is just to make the compiler happy
                        else -> false
                    }
                }

                !destination.exists() -> {
                    pullFolder(destination, serial)
                }

                else -> {
                    throw PushFailedException("Destination $destination is not a directory or a file")
                }
            }
        }

    private suspend fun AndroidDebugBridgeClient.pullFolder(
        destination: File,
        serial: String?
    ): Boolean {

        val (filesToPull, _) = BFFSearch<String, File>().execute(
            source,
            destination
        ) { currentDir, newDirs, newFiles, _, destinationRoot ->
            val ls = execute(CompatListFileRequest(currentDir, supportedFeatures), serial)
            for (file in ls.filterNot { Const.SYNC_IGNORED_FILES.contains(it.name) }) {
                when {
                    file.isDirectory() -> newDirs.add(currentDir + Const.ANDROID_FILE_SEPARATOR + file.name)
                    file.isRegularFile() && file.size() == 0L.toULong() -> {
                        val remotePath = currentDir + Const.ANDROID_FILE_SEPARATOR + file.name
                        val remoteRelativePath = remotePath.substringAfter(source)
                        val localRelativePath = remoteRelativePath.replace(Const.ANDROID_FILE_SEPARATOR, File.separator)
                        val local = File(destinationRoot.absolutePath, localRelativePath)
                        local.parentFile.mkdirs()
                        local.createNewFile()
                    }

                    file.isRegularFile() || file.isCharDevice() || file.isBlockDevice() -> {
                        val remotePath = currentDir + Const.ANDROID_FILE_SEPARATOR + file.name
                        val remoteRelativePath = remotePath.substringAfter(source)
                        val localRelativePath = remoteRelativePath.replace(Const.ANDROID_FILE_SEPARATOR, File.separator)
                        newFiles.add(
                            SyncFile(
                                local = File(destinationRoot.absolutePath, localRelativePath),
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

    private suspend fun AndroidDebugBridgeClient.doPullFile(
        source: String,
        realDestination: File,
        size: Long,
        serial: String?
    ): Boolean {
        val channel = execute(
            CompatPullFileRequest(source, realDestination, supportedFeatures, this@PullRequest, size, coroutineContext),
            serial
        )
        var progress = 0.0
        for (update in channel) {
            progress = update
        }
        return progress == 1.0
    }
}
