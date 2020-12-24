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

package com.malinskiy.adam.request.shell.v2

enum class MessageType {
    STDIN,
    STDOUT,
    STDERR,
    EXIT,

    /**
     * Close subprocess stdin if possible
     */
    CLOSE_STDIN,

    /**
     * Window size change (an ASCII version of struct winsize)
     */
    WINDOW_SIZE_CHANGE,

    /**
     * Indicates an invalid or unknown packet
     */
    INVALID;

    companion object {
        fun of(value: Int) = when (value) {
            0 -> STDIN
            1 -> STDOUT
            2 -> STDERR
            3 -> EXIT
            4 -> CLOSE_STDIN
            5 -> WINDOW_SIZE_CHANGE
            else -> INVALID
        }
    }
}