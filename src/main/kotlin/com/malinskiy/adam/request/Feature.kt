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

package com.malinskiy.adam.request

/**
 * Each entry represents a service that is supported by the adbd on the device
 */
enum class Feature {
    SHELL_V2,

    /**
     * The 'cmd' command is available
     */
    CMD,
    STAT_V2,
    LS_V2,

    /**
     * The server is running with libusb enabled
     */
    LIBUSB,

    /**
     * adbd supports `push --sync`
     */
    PUSH_SYNC,

    /**
     * adbd supports installing .apex packages.
     */
    APEX,

    /**
     * adbd has b/110953234 fixed.
     */
    FIXED_PUSH_MKDIR,

    /**
     * adbd supports android binder bridge (abb) in interactive mode using shell protocol.
     */
    ABB,

    /**
     * adbd properly updates symlink timestamps on push.
     */
    FIXED_PUSH_SYMLINK_TIMESTAMP,

    /**
     * adbd supports abb using raw pipe.
     */
    ABB_EXEC,

    /**
     * Implement `adb remount` via shelling out to /system/bin/remount.
     */
    REMOUNT_SHELL,

    /**
     * adbd supports `track-app` service reporting debuggable/profileable apps.
     */
    TRACK_APP,

    /**
     * adbd supports version 2 of send/recv.
     */
    SENDRECV_V2,

    /**
     * adbd supports brotli for send/recv v2.
     */
    SENDRECV_V2_BROTLI,

    /**
     * adbd supports LZ4 for send/recv v2.
     */
    SENDRECV_V2_LZ4,

    /**
     * adbd supports Zstd for send/recv v2.
     */
    SENDRECV_V2_ZSTD,

    /**
     * adbd supports dry-run send for send/recv v2.
     */
    SENDRECV_V2_DRY_RUN_SEND;

    companion object {
        /**
         * see adb/transport.cpp for up-to-date list
         */
        fun of(value: String) = when (value) {
            "shell_v2" -> SHELL_V2
            "cmd" -> CMD
            "stat_v2" -> STAT_V2
            "ls_v2" -> LS_V2
            "libusb" -> LIBUSB
            "push_sync" -> PUSH_SYNC
            "apex" -> APEX
            "fixed_push_mkdir" -> FIXED_PUSH_MKDIR
            "abb" -> ABB
            "fixed_push_symlink_timestamp" -> FIXED_PUSH_SYMLINK_TIMESTAMP
            "abb_exec" -> ABB_EXEC
            "remount_shell" -> REMOUNT_SHELL
            "track_app" -> TRACK_APP
            "sendrecv_v2" -> SENDRECV_V2
            "sendrecv_v2_brotli" -> SENDRECV_V2_BROTLI
            "sendrecv_v2_lz4" -> SENDRECV_V2_LZ4
            "sendrecv_v2_zstd" -> SENDRECV_V2_ZSTD
            "sendrecv_v2_dry_run_send" -> SENDRECV_V2_DRY_RUN_SEND
            else -> null
        }
    }
}
