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

class ShellV2SubSession(private val session: Session) {
    suspend fun accept(): ShellV2SubSession {
        session.respondOkay()
        return this
    }

    suspend fun respondStdout(stdout: String): ShellV2SubSession {
        session.respondShellV2Stdout(stdout)
        return this
    }

    suspend fun respondStderr(stdout: String): ShellV2SubSession {
        session.respondShellV2Stderr(stdout)
        return this
    }

    suspend fun respondExit(exitCode: Int): ShellV2SubSession {
        session.respondShellV2Exit(exitCode)
        return this
    }

    suspend fun reject(message: String) {
        session.respondTransport(false, message)
    }
}
