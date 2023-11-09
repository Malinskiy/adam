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

package com.malinskiy.adam.integration

import com.android.emulator.control.EmulatorControllerGrpcKt
import com.google.protobuf.Empty
import com.malinskiy.adam.rule.AdbDeviceRule
import com.malinskiy.adam.rule.DeviceType
import io.grpc.CallCredentials
import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.ClientInterceptor
import io.grpc.ClientInterceptors
import io.grpc.ManagedChannelBuilder
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import io.grpc.Status
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.Executor

class EmulatorGrpcE2ETest {
    @Rule
    @JvmField
    val emulator = AdbDeviceRule(deviceType = DeviceType.EMULATOR)

    @Test
    fun testProto() {
        runBlocking {
            val adbPort = emulator.deviceSerial.substringAfter('-').toInt()
            val channel = ManagedChannelBuilder.forAddress("localhost", adbPort + 3000).apply {
                usePlaintext()
                executor(Dispatchers.IO.asExecutor())
            }.build()

            val emulator = EmulatorControllerGrpcKt.EmulatorControllerCoroutineStub(channel)
                .withCallCredentials(TokenCallCredentials("Kz0Xqi/UYukUfI1qykyK84h0DETm44xvpgQ+tQDtQpfe/59QG+DbtaG5iy4Pe4atFBq18vfY9psJ1rmFuM9hig=="))
            val status = emulator.getStatus(Empty.getDefaultInstance())
            println(status)
        }
    }
}

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
private class TokenCallCredentials(private val token: String) : CallCredentials() {
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
