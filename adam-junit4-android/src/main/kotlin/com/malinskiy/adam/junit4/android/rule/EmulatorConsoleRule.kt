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

package com.malinskiy.adam.junit4.android.rule

import androidx.test.platform.app.InstrumentationRegistry
import com.malinskiy.adam.AndroidDebugBridgeClient
import com.malinskiy.adam.AndroidDebugBridgeClientFactory
import com.malinskiy.adam.junit4.android.contract.TestRunnerContract
import com.malinskiy.adam.request.emu.EmulatorCommandRequest
import kotlinx.coroutines.Dispatchers
import org.junit.AssumptionViolatedException
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.net.InetSocketAddress
import kotlin.coroutines.CoroutineContext

/**
 * @param coroutineContext it's your responsibility to cancel this context when needed
 */
class EmulatorConsoleRule(private val mode: Mode = Mode.ASSERT, private val coroutineContext: CoroutineContext = Dispatchers.IO) :
    TestRule {
    private lateinit var client: AndroidDebugBridgeClient
    private lateinit var inetSocketAddress: InetSocketAddress
    private lateinit var authToken: String

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                val arguments = InstrumentationRegistry.getArguments()
                val port = arguments.getString(TestRunnerContract.consolePortArgumentName)?.toIntOrNull()
                val host = arguments.getString(TestRunnerContract.consoleHostArgumentName)
                authToken = arguments.getString(TestRunnerContract.emulatorAuthTokenArgumentName) ?: ""

                if (port != null && host != null) {
                    client = AndroidDebugBridgeClientFactory().apply {
                        coroutineContext = this@EmulatorConsoleRule.coroutineContext
                    }.build()
                    inetSocketAddress = InetSocketAddress(host, port)
                } else {
                    when (mode) {
                        Mode.SKIP -> return
                        Mode.ASSUME -> throw AssumptionViolatedException("No access to console port: host = $host, port = $port, token = $authToken")
                        Mode.ASSERT -> throw AssertionError("No access to console port: host = $host, port = $port, token = $authToken")
                    }
                }

                try {
                    base.evaluate()
                } finally {
                    client.socketFactory.close()
                }
            }
        }
    }

    suspend fun execute(cmd: String) = client.execute(EmulatorCommandRequest(cmd, inetSocketAddress, authToken))
}
