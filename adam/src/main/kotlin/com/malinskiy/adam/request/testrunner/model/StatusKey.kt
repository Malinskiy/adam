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

package com.malinskiy.adam.request.testrunner.model

enum class StatusKey(val value: String) {
    TEST("test"),
    CLASS("class"),
    STACK("stack"),
    NUMTESTS("numtests"),
    ERROR("Error"),
    SHORTMSG("shortMsg"),
    STREAM("stream"),
    CURRENT("current"),
    ID("id"),
    UNKNOWN("");

    companion object {
        fun of(value: String?) = when (value) {
            "test" -> TEST
            "class" -> CLASS
            "stack" -> STACK
            "numtests" -> NUMTESTS
            "Error" -> ERROR
            "shortMsg" -> SHORTMSG
            "stream" -> STREAM
            "current" -> CURRENT
            "id" -> ID
            else -> UNKNOWN
        }
    }
}
