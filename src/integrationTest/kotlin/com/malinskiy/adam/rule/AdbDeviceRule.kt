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
import com.malinskiy.adam.request.devices.Device
import com.malinskiy.adam.request.devices.DeviceState
import com.malinskiy.adam.request.devices.ListDevicesRequest
import com.malinskiy.adam.request.sync.GetSinglePropRequest
import kotlinx.coroutines.runBlocking
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.net.ConnectException

class AdbDeviceRule : TestRule {
    val deviceSerial = System.getenv("ADAM_INTEGRATION_DEVICE_SERIAL") ?: "emulator-5554"
    val adb = AndroidDebugBridgeClientFactory().build()

    override fun apply(base: Statement?, description: Description?): Statement {
        return object : Statement() {
            override fun evaluate() {
                runBlocking {
                    StartAdbInteractor().execute()

                    //Wait for device to be available on adb server
                    while (true) {
                        try {
                            val output = adb.execute(ListDevicesRequest())
                            if (output.contains(Device(deviceSerial, DeviceState.DEVICE))) {
                                break
                            }
                        } catch (e: ConnectException) {
                            continue
                        }
                    }

                    //Wait for device boot
                    while (true) {
                        try {
                            val completed = adb.execute(GetSinglePropRequest("sys.boot_completed"))
                            if (completed.isNotEmpty()) break
                            continue
                        } catch (e: ConnectException) {
                            continue
                        }
                    }
                }
                base?.evaluate()
            }
        }
    }
}