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

package com.malinskiy.adam

object Const {
    const val READ_DELAY = 100L
    val DEFAULT_TRANSPORT_ENCODING = Charsets.ISO_8859_1
    const val DEFAULT_ADB_HOST = "127.0.0.1"
    const val DEFAULT_ADB_PORT = 5037

    const val SERVER_PORT_ENV_VAR = "ANDROID_ADB_SERVER_PORT"
    const val MAX_PACKET_LENGTH = 16384
}