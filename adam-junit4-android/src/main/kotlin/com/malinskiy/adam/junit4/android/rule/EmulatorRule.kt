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

package com.malinskiy.adam.junit4.rule

import androidx.test.platform.app.InstrumentationRegistry
import com.android.emulator.control.EmulatorControllerGrpcKt
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.util.concurrent.TimeUnit

class EmulatorRule : TestRule {
    var grpc: EmulatorControllerGrpcKt.EmulatorControllerCoroutineStub? = null
    private var channel: ManagedChannel? = null

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                val arguments = InstrumentationRegistry.getArguments()
                val grpcPort = arguments.getString("ADAM_GRPC_PORT")?.toIntOrNull()
                val adbPort = arguments.getString("ADAM_ADB_PORT")?.toIntOrNull()
                val consolePort = arguments.getString("ADAM_CONSOLE_PORT")?.toIntOrNull()

                if (grpcPort != null) {
                    channel = ManagedChannelBuilder.forAddress("localhost", 8554).apply {
                        usePlaintext()
                        executor(Dispatchers.IO.asExecutor())
                    }.build()
                    grpc = EmulatorControllerGrpcKt.EmulatorControllerCoroutineStub(channel!!)
                } else {
                    grpc = null
                }

                try {
                    base.evaluate()
                } finally {
                    channel?.shutdownNow()
                    channel?.awaitTermination(5, TimeUnit.SECONDS)
                }
            }
        }
    }
}