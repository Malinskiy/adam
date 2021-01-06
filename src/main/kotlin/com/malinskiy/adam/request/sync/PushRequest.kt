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
import com.malinskiy.adam.exception.PushFailedException
import com.malinskiy.adam.request.Feature
import com.malinskiy.adam.request.MultiRequest
import com.malinskiy.adam.request.sync.compat.CompatPushFileRequest
import com.malinskiy.adam.request.sync.compat.CompatStatFileRequest
import com.malinskiy.adam.request.sync.model.SyncFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.io.File
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
class PushRequest(
    private val source: File,
    private val destination: String,
    private val supportedFeatures: List<Feature>,
    private val mode: String = "0777",
    override val coroutineContext: CoroutineContext = Dispatchers.IO
) : MultiRequest<Boolean>(), CoroutineScope {

    /**
     * @return true if successful, false if not. false can be a partial success: some files might be pulled
     */
    override suspend fun execute(androidDebugBridgeClient: AndroidDebugBridgeClient, serial: String?): Boolean =
        with(androidDebugBridgeClient) {
            val remoteFileEntry = execute(CompatStatFileRequest(destination, supportedFeatures), serial)

            when {
                source.isFile -> {
                    when {
                        remoteFileEntry.isDirectory() -> {
                            //Put into folder
                            doPushFile(
                                source,
                                destination.removeSuffix(Const.ANDROID_FILE_SEPARATOR) + Const.ANDROID_FILE_SEPARATOR + source.name,
                                mode,
                                serial
                            )
                        }
                        remoteFileEntry.isRegularFile() || !remoteFileEntry.exists() -> {
                            doPushFile(source, destination, mode, serial)
                        }
                        else -> {
                            throw PushFailedException(
                                "Target $destination exists and is not a directory or a file: mode=${
                                    remoteFileEntry.mode.toString(
                                        8
                                    )
                                }"
                            )
                        }
                    }
                }
                source.isDirectory -> {
                    when {
                        remoteFileEntry.isDirectory() -> {
                            //Put into subfolder
                            pushFolder(
                                destination.removeSuffix(Const.ANDROID_FILE_SEPARATOR) + Const.ANDROID_FILE_SEPARATOR + source.name,
                                serial
                            )
                        }
                        remoteFileEntry.isRegularFile() -> {
                            throw PushFailedException("Can't push folder $source: target $destination is a file")
                        }
                        !remoteFileEntry.exists() -> {
                            pushFolder(destination.removeSuffix(Const.ANDROID_FILE_SEPARATOR), serial)
                        }
                        else -> {
                            throw PushFailedException(
                                "Target $destination exists and is not a directory or a file: mode=${
                                    remoteFileEntry.mode.toString(
                                        8
                                    )
                                }"
                            )
                        }
                    }
                }
                !source.exists() -> {
                    throw PushFailedException("Source $source doesn't exist")
                }
                else -> {
                    throw PushFailedException("Source $source is not a directory or a file")
                }
            }
        }

    /**
     * @param destination might not exist. doesn't have file separator at end
     */
    private suspend fun AndroidDebugBridgeClient.pushFolder(destination: String, serial: String?): Boolean {
        val filesToPush = BFFSearch<File, String>().execute(
            source,
            destination
        ) { currentDir, newDirs, newFiles, destinationRoot ->
            val ls = currentDir.listFiles()?.filterNot { Const.SYNC_IGNORED_FILES.contains(it.name) }
            if (ls.isNullOrEmpty()) return@execute
            for (file in ls) {
                when {
                    file.isDirectory -> newDirs.add(file)
                    file.isFile -> {
                        val localRelativePath = file.toRelativeString(source)
                        val remoteRelativePath = localRelativePath.replace(File.separator, Const.ANDROID_FILE_SEPARATOR)
                        newFiles.add(
                            SyncFile(
                                local = file,
                                remote = destinationRoot + Const.ANDROID_FILE_SEPARATOR + remoteRelativePath,
                                mtime = file.lastModified() / 1000,
                                mode = mode.toUInt(8) and "0777".toUInt(8),
                                size = file.length().toULong()
                            )
                        )
                    }
                }
            }
        }

        filesToPush.forEach { file ->
            val fileSuccess = doPushFile(file.local, file.remote, mode, serial)
            if (!fileSuccess) return false
        }

        return true
    }

    private suspend fun AndroidDebugBridgeClient.doPushFile(
        source: File,
        destination: String,
        mode: String,
        serial: String?
    ): Boolean {
        val channel = execute(
            CompatPushFileRequest(source, destination, mode, supportedFeatures, this@PushRequest, coroutineContext),
            serial
        )
        var progress = 0.0
        for (update in channel) {
            progress = update
        }
        return progress == 1.0
    }
}
