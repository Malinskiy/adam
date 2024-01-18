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
import com.malinskiy.adam.grpc.auth.TokenCallCredentials
import com.malinskiy.adam.rule.AdbDeviceRule
import com.malinskiy.adam.rule.DeviceType
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import java.util.*
import kotlin.io.path.Path

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
            val status = emulator.getStatus(Empty.getDefaultInstance())
            println(status)
        }
    }

    @Test
    fun testProtoWithLocalToken() {
        runBlocking {
            val adbPort = emulator.deviceSerial.substringAfter('-').toInt()
            val channel = ManagedChannelBuilder.forAddress("localhost", adbPort + 3000).apply {
                usePlaintext()
                executor(Dispatchers.IO.asExecutor())
            }.build()

            // Providing a token is outside the scope of adam's responsibilities
            // For a locally running emulator (only one) and this test running on the same host the following can be used
            val running = Path(System.getenv("XDG_RUNTIME_DIR"), "avd", "running").toFile()
            val iniFiles = running.listFiles { _, name -> name.endsWith("ini") }
            assert(iniFiles.size == 1)
            val avdPropertiesFile = iniFiles.first()
            val avdProperties = Properties()
            avdProperties.load(avdPropertiesFile.reader())
            val emulator = EmulatorControllerGrpcKt.EmulatorControllerCoroutineStub(channel)
                .withCallCredentials(TokenCallCredentials(avdProperties.getProperty("grpc.token")))
            val status = emulator.getStatus(Empty.getDefaultInstance())
            println(status)
        }
    }
}
