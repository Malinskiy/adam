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
import com.malinskiy.adam.AndroidDebugBridgeClient
import com.malinskiy.adam.AndroidDebugBridgeClientFactory
import com.malinskiy.adam.junit4.android.UnsafeAdbAccess
import com.malinskiy.adam.junit4.android.contract.TestRunnerContract
import com.malinskiy.adam.junit4.android.rule.Mode
import com.malinskiy.adam.junit4.android.rule.sandbox.SingleTargetAndroidDebugBridgeClient
import kotlinx.coroutines.Dispatchers
import org.junit.AssumptionViolatedException
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.net.InetAddress
import kotlin.coroutines.CoroutineContext

/**
 * @param coroutineContext it's your responsibility to cancel this context when needed
 */
class AdbRule(private val mode: Mode = Mode.ASSERT, private val coroutineContext: CoroutineContext = Dispatchers.IO) :
    TestRule {
    lateinit var adb: SingleTargetAndroidDebugBridgeClient

    @UnsafeAdbAccess
    lateinit var adbUnsafe: AndroidDebugBridgeClient

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                val arguments = InstrumentationRegistry.getArguments()
                val adbPort = arguments.getString(TestRunnerContract.adbPortArgumentName)?.toIntOrNull()
                val adbHost = arguments.getString(TestRunnerContract.adbHostArgumentName)
                val serial = arguments.getString(TestRunnerContract.deviceSerialArgumentName)

                if (adbPort != null && adbHost != null && serial != null) {
                    adbUnsafe = AndroidDebugBridgeClientFactory().apply {
                        port = adbPort
                        host = InetAddress.getByName(adbHost)
                        coroutineContext = coroutineContext
                    }.build()

                    adb = SingleTargetAndroidDebugBridgeClient(adbUnsafe, serial)
                } else {
                    when (mode) {
                        Mode.SKIP -> return
                        Mode.ASSUME -> throw AssumptionViolatedException("No access to adb port or device serial has been provided")
                        Mode.ASSERT -> throw AssertionError("No access to adb port or device serial has been provided")
                    }
                }

                try {
                    base.evaluate()
                } finally {
                    adbUnsafe.socketFactory.close()
                }
            }
        }
    }
}
