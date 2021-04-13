/*
 * Copyright (C) 2019 Anton Malinskiy
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

package com.malinskiy.adam.rule

import com.malinskiy.adam.AndroidDebugBridgeClientFactory
import com.malinskiy.adam.interactor.StartAdbInteractor
import com.malinskiy.adam.request.Feature
import com.malinskiy.adam.request.device.Device
import com.malinskiy.adam.request.device.FetchDeviceFeaturesRequest
import com.malinskiy.adam.request.device.ListDevicesRequest
import com.malinskiy.adam.request.misc.GetAdbServerVersionRequest
import com.malinskiy.adam.request.prop.GetSinglePropRequest
import com.malinskiy.adam.request.shell.v1.ShellCommandRequest
import com.malinskiy.adam.transport.roket.RoketFactory
import com.malinskiy.adam.transport.vertx.VertxSocketFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assume
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.net.ConnectException
import java.time.Duration

/**
 * This rule supports only one device
 *
 * If device is not found - error
 * If device doesn't have required features - assumption failure
 */
class AdbDeviceRule(val deviceType: DeviceType = DeviceType.ANY, vararg val requiredFeatures: Feature) : TestRule {
    lateinit var deviceSerial: String
    lateinit var supportedFeatures: List<Feature>
    lateinit var lineSeparator: String

    val adb = AndroidDebugBridgeClientFactory().build()
    val initTimeout = Duration.ofSeconds(10)

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                runBlocking {
                    withTimeoutOrNull(initTimeout.toMillis()) {
                        //First we start the adb if it is not running
                        startAdb()

                        //Wait for compatible device
                        //boot + supported features
                        val device = waitForDevice()
                        deviceSerial = device.serial
                    } ?: throw RuntimeException("Timeout waiting for device")
                }
                base.evaluate()
            }
        }
    }

    private suspend fun CoroutineScope.waitForDevice(): Device {
        while (isActive) {
            try {
                loop@ for (device in adb.execute(ListDevicesRequest())) {
                    val booted = adb.execute(GetSinglePropRequest("sys.boot_completed"), device.serial).isNotBlank()
                    if (!booted) continue

                    when (deviceType) {
                        DeviceType.EMULATOR -> {
                            Assume.assumeTrue(
                                "No device of type $deviceType found",
                                device.serial.startsWith("emulator-")
                            )
                        }
                    }

                    supportedFeatures = adb.execute(FetchDeviceFeaturesRequest(device.serial))
                    if (!requiredFeatures.isEmpty()) {
                        Assume.assumeTrue(
                            "No compatible device found for features $requiredFeatures",
                            requiredFeatures.isEmpty() ||
                                    supportedFeatures.containsAll(requiredFeatures.asList())
                        )
                    }

                    lineSeparator = adb.execute(
                        ShellCommandRequest("echo"),
                        device.serial
                    ).output

                    return device
                }
            } catch (e: ConnectException) {
                continue
            }
        }
        throw RuntimeException("Timeout waiting for device")
    }

    private suspend fun startAdb() {
        try {
            adb.execute(GetAdbServerVersionRequest())
        } catch (e: ConnectException) {
            val success = StartAdbInteractor().execute()
            if (!success) {
                throw RuntimeException("Unable to start adb")
            }
        }
    }
}
