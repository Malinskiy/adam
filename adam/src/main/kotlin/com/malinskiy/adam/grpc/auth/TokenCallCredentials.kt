/*
 * Copyright (C) 2024 Anton Malinskiy
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

package com.malinskiy.adam.grpc.auth

import io.grpc.CallCredentials
import io.grpc.Metadata
import io.grpc.Status
import java.util.concurrent.Executor

/**
 *   Require an authorization header with a valid token for every grpc call.
 *
 *   Every grpc request expects the following header:
 *
 *     authorization: Bearer <token>
 *
 *   The token can be found in the android studio discovery file under the key: grpc.token.
 *   If an incorrect token is present the status UNAUTHORIZED will be returned.
 *
 *   The location of the discovery directory is $XDG_RUNTIME_DIR/avd/running
 *   The file will be named `pid_%d_info.ini` where %d is the process id of the emulator.
 *
 *   Note: Token based security can only be installed if you are using localhost or TLS.
 *
 */
class TokenCallCredentials(private val token: String) : CallCredentials() {
    private val AUTHORIZATION_METADATA_KEY = Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER)
    override fun applyRequestMetadata(requestInfo: RequestInfo, executor: Executor, applier: MetadataApplier) {
        executor.execute {
            try {
                val headers = Metadata()
                headers.put(AUTHORIZATION_METADATA_KEY, "Bearer $token")
                applier.apply(headers)
            } catch (e: Throwable) {
                applier.fail(Status.UNAUTHENTICATED.withCause(e))
            }
        }
    }

    override fun thisUsesUnstableApi() {
    }
}