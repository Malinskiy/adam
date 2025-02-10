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

import com.malinskiy.adam.request.Feature
import com.malinskiy.adam.request.shell.AsyncCompatShellCommandRequest
import com.malinskiy.adam.request.shell.v2.ShellCommandResultChunk
import com.malinskiy.adam.request.transform.DumpResponseTransformer
import com.malinskiy.adam.request.transform.InstrumentationResponseTransformer
import com.malinskiy.adam.request.transform.ProgressiveResponseTransformer
import com.malinskiy.adam.request.transform.ProtoInstrumentationResponseTransformer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.SendChannel
import java.io.File

/**
 * @param outputLogPath if specified with protobuf then write output as protobuf to a file (machine
 * readable). If path is not specified, default directory and file name will
 * be used: /sdcard/instrument-logs/log-yyyyMMdd-hhmmss-SSS.instrumentation_data_proto
 *
 * @param protobuf API 26+
 *
 * @param noIsolatedStorage don't use isolated storage sandbox and mount full external storage
 * @param noHiddenApiChecks disable restrictions on use of hidden API
 * @param noWindowAnimations turn off window animations while running
 * @param userId Specify user instrumentation runs in; current user if not specified
 * @param abi Launch the instrumented process with the selected ABI. This assumes that the process supports the selected ABI.
 * @param profilingOutputPath write profiling data to <FILE>
 * @param socketIdleTimeout override for socket idle timeout. This should be longer than the longest test
 *
 * @see https://android.googlesource.com/platform/frameworks/base/+/master/cmds/am/src/com/android/commands/am/Am.java#155
 */
class TestRunnerRequest(
    private val testPackage: String,
    private val instrumentOptions: InstrumentOptions,
    private val supportedFeatures: List<Feature>,
    private val coroutineScope: CoroutineScope,
    private val runnerClass: String = "android.support.test.runner.AndroidJUnitRunner",
    private val noHiddenApiChecks: Boolean = false,
    private val noWindowAnimations: Boolean = false,
    private val noIsolatedStorage: Boolean = false,
    private val userId: Int? = null,
    private val abi: String? = null,
    private val profilingOutputPath: String? = null,
    private val outputLogPath: String? = null,
    private val protobuf: Boolean = false,
    private val dumpFile: File? = null,
    socketIdleTimeout: Long? = Long.MAX_VALUE
) : AsyncCompatShellCommandRequest<List<TestEvent>>(
    cmd = StringBuilder().apply {
        append("am instrument -w -r")

        if (noHiddenApiChecks) {
            append(" --no-hidden-api-checks")
        }

        if (noWindowAnimations) {
            append(" --no-window-animation")
        }

        if (noIsolatedStorage) {
            append(" --no-isolated-storage")
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

        if (protobuf) {
            append(" -m")
        }

        if (outputLogPath != null) {
            append(" -f $outputLogPath")
        }

        append(instrumentOptions.toString())

        append(" $testPackage/$runnerClass")
    }.toString(),
    supportedFeatures = supportedFeatures,
    coroutineScope = coroutineScope,
    socketIdleTimeout = socketIdleTimeout,
) {
    private val transformer: ProgressiveResponseTransformer<List<TestEvent>?> by lazy {
        val raw = if (protobuf) {
            ProtoInstrumentationResponseTransformer()
        } else {
            InstrumentationResponseTransformer()
        }

        if (dumpFile != null) {
            if (dumpFile.exists()) {
                dumpFile.delete()
            }
            DumpResponseTransformer(dumpFile, raw)
        } else {
            raw
        }
    }

    override suspend fun convertChunk(response: ShellCommandResultChunk): List<TestEvent>? {
        return response.stdout?.let { bytes ->
            transformer.process(bytes, 0, bytes.size)
        }
    }

    override suspend fun close(channel: SendChannel<List<TestEvent>>) {
        transformer.transform()?.let { channel.send(it) }
    }
}
