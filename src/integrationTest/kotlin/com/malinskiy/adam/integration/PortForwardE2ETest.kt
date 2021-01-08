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
import assertk.assertions.*
import com.malinskiy.adam.request.forwarding.*
import com.malinskiy.adam.request.reverse.ListReversePortForwardsRequest
import com.malinskiy.adam.request.reverse.RemoveAllReversePortForwardsRequest
import com.malinskiy.adam.request.reverse.RemoveReversePortForwardRequest
import com.malinskiy.adam.request.reverse.ReversePortForwardRequest
import com.malinskiy.adam.rule.AdbDeviceRule
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class PortForwardE2ETest {

    @Rule
    @JvmField
    val adbRule = AdbDeviceRule()

    @Before
    fun setup() {
        runBlocking {
            adbRule.adb.execute(RemoveAllPortForwardsRequest(adbRule.deviceSerial))
            adbRule.adb.execute(RemoveAllReversePortForwardsRequest(), adbRule.deviceSerial)
        }
    }

    @After
    fun teardown() {
        runBlocking {
            adbRule.adb.execute(RemoveAllPortForwardsRequest(adbRule.deviceSerial))
            adbRule.adb.execute(RemoveAllReversePortForwardsRequest(), adbRule.deviceSerial)
        }
    }

    @Test
    fun testPortForward() {
        runBlocking {
            adbRule.adb.execute(
                PortForwardRequest(LocalTcpPortSpec(12042), RemoteTcpPortSpec(12042), serial = adbRule.deviceSerial),
                adbRule.deviceSerial
            )

            val portForwards = adbRule.adb.execute(
                ListPortForwardsRequest(adbRule.deviceSerial),
                adbRule.deviceSerial
            )

            assertThat(portForwards).hasSize(1)
            val rule = portForwards[0]
            assertThat(rule.serial).isEqualTo(adbRule.deviceSerial)
            assertThat(rule.localSpec).isInstanceOf(LocalTcpPortSpec::class)
            assertThat((rule.localSpec as LocalTcpPortSpec).port).isEqualTo(12042)

            assertThat(rule.remoteSpec).isInstanceOf(RemoteTcpPortSpec::class)
            assertThat((rule.remoteSpec as RemoteTcpPortSpec).port).isEqualTo(12042)

            adbRule.adb.execute(RemovePortForwardRequest(LocalTcpPortSpec(12042), serial = adbRule.deviceSerial), adbRule.deviceSerial)

            val afterAllForwards = adbRule.adb.execute(
                ListPortForwardsRequest(adbRule.deviceSerial), adbRule.deviceSerial
            )

            assertThat(afterAllForwards).isEmpty()
        }
    }

    @Test
    fun testPortForwardTcpPortAllocation() {
        runBlocking {
            val portString = adbRule.adb.execute(
                PortForwardRequest(LocalTcpPortSpec(), RemoteTcpPortSpec(12042), serial = adbRule.deviceSerial),
                adbRule.deviceSerial
            )

            val currentPortForwards = adbRule.adb.execute(
                ListPortForwardsRequest(adbRule.deviceSerial), adbRule.deviceSerial
            )

            assertThat(portString).isNotNull()
            val actual = portString!!.toInt()
            val expected = (currentPortForwards.firstOrNull()?.localSpec as LocalTcpPortSpec).port

            assertThat(actual).isEqualTo(expected)
        }
    }

    @Test
    fun testPortForwardKillSingle() {
        runBlocking {
            adbRule.adb.execute(
                PortForwardRequest(LocalTcpPortSpec(12042), RemoteTcpPortSpec(12042), serial = adbRule.deviceSerial),
                adbRule.deviceSerial
            )

            adbRule.adb.execute(
                RemovePortForwardRequest(LocalTcpPortSpec(12042), serial = adbRule.deviceSerial),
                adbRule.deviceSerial
            )

            val portForwards = adbRule.adb.execute(
                ListPortForwardsRequest(adbRule.deviceSerial),
                adbRule.deviceSerial
            )

            assertThat(portForwards).isEmpty()
        }
    }

    @Test
    fun testReversePortForward() = runBlocking {
        adbRule.adb.execute(RemoveAllReversePortForwardsRequest(), adbRule.deviceSerial)

        assertThat(adbRule.adb.execute(ListReversePortForwardsRequest(), adbRule.deviceSerial)).isEmpty()

        adbRule.adb.execute(
            ReversePortForwardRequest(RemoteTcpPortSpec(12043), LocalTcpPortSpec(12043)),
            adbRule.deviceSerial
        )

        val list = adbRule.adb.execute(ListReversePortForwardsRequest(), adbRule.deviceSerial)
        assertThat(list).hasSize(1)
        //Host serial is of form "host-$tid", we ignore it here for equality check
        assertThat(list.first().localSpec).isEqualTo(RemoteTcpPortSpec(12043))
        assertThat(list.first().remoteSpec).isEqualTo(LocalTcpPortSpec(12043))

        adbRule.adb.execute(
            RemoveReversePortForwardRequest(RemoteTcpPortSpec(12043)),
            adbRule.deviceSerial
        )

        assertThat(adbRule.adb.execute(ListReversePortForwardsRequest(), adbRule.deviceSerial)).isEmpty()
    }

    @Test
    fun testReversePortForwardTcpPortAllocation() {
        runBlocking {
            val portString = adbRule.adb.execute(
                ReversePortForwardRequest(RemoteTcpPortSpec(12042), LocalTcpPortSpec(12042)),
                adbRule.deviceSerial
            )

            val currentPortForwards = adbRule.adb.execute(
                ListReversePortForwardsRequest(), adbRule.deviceSerial
            )

            if (portString != null) {
                //Some devices return the allocated port
                val actual = portString.toInt()
                val expected = (currentPortForwards.firstOrNull()?.localSpec as RemoteTcpPortSpec).port

                assertThat(actual).isEqualTo(expected)
            } else {
                //Others do not
                val actual = currentPortForwards.any {
                    it.remoteSpec == LocalTcpPortSpec(12042) && it.localSpec == RemoteTcpPortSpec(12042)
                }
                assertThat(actual).isTrue()
            }
        }
    }
}
