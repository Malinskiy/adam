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

import assertk.assertThat
import assertk.assertions.startsWith
import com.android.emulator.control.*
import com.google.protobuf.Empty
import com.malinskiy.adam.request.emu.EmulatorCommandRequest
import com.malinskiy.adam.rule.AdbDeviceRule
import com.malinskiy.adam.rule.DeviceType
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import java.net.InetSocketAddress

class EmulatorCommandRequestTest {
    @Rule
    @JvmField
    val adbRule = AdbDeviceRule(deviceType = DeviceType.EMULATOR)

    @Test
    fun testHelpCommand() {
        runBlocking {
            val port = adbRule.deviceSerial.substringAfter('-').toInt()

            val output = adbRule.adb.execute(
                EmulatorCommandRequest(
                    "help",
                    InetSocketAddress("localhost", port)
                )
            )
            assertThat(output).startsWith("Android console commands")
        }
    }

    @Test
    fun testProto() {
        runBlocking {
            val channel = ManagedChannelBuilder.forAddress("localhost", 8554).apply {
                usePlaintext()
                executor(Dispatchers.IO.asExecutor())
            }.build()

            val emulator = EmulatorControllerGrpcKt.EmulatorControllerCoroutineStub(channel)
            val status = emulator.getStatus(Empty.getDefaultInstance())
            println(status)
        }
    }
}
