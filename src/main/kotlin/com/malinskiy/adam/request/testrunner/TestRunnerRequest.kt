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

package com.malinskiy.adam.request.testrunner

import com.malinskiy.adam.Const
import com.malinskiy.adam.request.async.AsyncChannelRequest
import com.malinskiy.adam.transport.AndroidReadChannel
import com.malinskiy.adam.transport.AndroidWriteChannel

/**
 * @see https://android.googlesource.com/platform/frameworks/base/+/master/cmds/am/src/com/android/commands/am/Am.java#155
 */
class TestRunnerRequest(
    private val testPackage: String,
    private val instrumentOptions: InstrumentOptions,
    private val runnerClass: String = "android.support.test.runner.AndroidJUnitRunner",
    private val noHiddenApiChecks: Boolean = false,
    private val noWindowAnimations: Boolean = false,
    private val userId: Int? = null,
    private val abi: String? = null,
    private val profilingOutputPath: String? = null,
    private val outputLogPath: String? = null
) : AsyncChannelRequest<String, Unit>() {
    private val buffer = ByteArray(Const.MAX_PACKET_LENGTH)

    override suspend fun readElement(readChannel: AndroidReadChannel, writeChannel: AndroidWriteChannel): String? {
        val available = readChannel.readAvailable(buffer, 0, Const.MAX_PACKET_LENGTH)

        return when {
            available > 0 -> {
                String(buffer, 0, available, Const.DEFAULT_TRANSPORT_ENCODING)
            }
            available < 0 -> {
                readChannel.cancel(null)
                writeChannel.close(null)
                return null
            }
            else -> null
        }
    }

    override fun serialize() = createBaseRequest(StringBuilder().apply {
        append("shell:")

        append("am instrument -w -r")

        if (noHiddenApiChecks) {
            append(" --no-hidden-api-checks")
        }

        if (noWindowAnimations) {
            append(" --no-window-animation")
        }

        if (userId != null) {
            append(" --user $userId")
        }

        if (abi != null) {
            append(" --abi $abi")
        }

        if (profilingOutputPath != null) {
            append(" -p $profilingOutputPath")
        }

        if (outputLogPath != null) {
            append(" -f $outputLogPath")
        }

        append(instrumentOptions.toString())

        append(" $testPackage/$runnerClass")
    }.toString())

    override suspend fun writeElement(element: Unit, readChannel: AndroidReadChannel, writeChannel: AndroidWriteChannel) = Unit
}
