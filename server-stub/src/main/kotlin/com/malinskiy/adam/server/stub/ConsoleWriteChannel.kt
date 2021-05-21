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

package com.malinskiy.adam.server.stub

import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.writeStringUtf8

class ConsoleWriteChannel(private val delegate: ByteWriteChannel) : ByteWriteChannel by delegate {
    suspend fun writeAuth() {
        delegate.writeStringUtf8("Android Console: Authentication required")
        delegate.writeStringUtf8("Android Console: type 'auth <auth_token>' to authenticate")
        delegate.writeStringUtf8("Android Console: you can find your <auth_token> in")
        delegate.writeStringUtf8("/<path-to-home>/.emulator_console_auth_token")
        delegate.writeStringUtf8("OK\r\n")
        delegate.writeStringUtf8("Android Console: type 'help' for a list of commands")
        delegate.writeStringUtf8("OK\r\n")
    }

    suspend fun respond(message: String) = delegate.writeStringUtf8(message)
}
