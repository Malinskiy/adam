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

package com.malinskiy.adam.integration

import com.malinskiy.adam.request.device.AsyncDeviceMonitorRequest
import com.malinskiy.adam.rule.AdbDeviceRule
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.runBlocking
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

@Ignore
class ManualTest {
    @Rule
    @JvmField
    val adbRule = AdbDeviceRule()

    @Test
    fun testDeviceMonitor() {
        runBlocking {
            val execute = adbRule.adb.execute(
                request = AsyncDeviceMonitorRequest(),
                scope = GlobalScope
            )
            for(i in 1..100) {
                println(execute.receive())
            }

            execute.cancel()
        }
    }
}
