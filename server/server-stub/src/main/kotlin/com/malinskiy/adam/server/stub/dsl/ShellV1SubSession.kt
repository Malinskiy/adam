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

package com.malinskiy.adam.server.stub.dsl

class ShellV1SubSession(private val session: Session) {
    suspend fun accept(): ShellV1SubSession {
        session.respondOkay()
        return this
    }

    suspend fun respond(stdout: String): ShellV1SubSession {
        session.respondShellV1(stdout)
        return this
    }

    suspend fun reject(message: String) {
        session.respondTransport(false, message)
    }
}
