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

package com.malinskiy.adam.request.transform

import com.android.commands.am.InstrumentationData
import com.google.protobuf.InvalidProtocolBufferException
import com.malinskiy.adam.Const
import com.malinskiy.adam.extension.*
import com.malinskiy.adam.request.testrunner.*
import com.malinskiy.adam.request.testrunner.model.*
import java.io.File
import java.io.RandomAccessFile
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * WARNING: the logcat field in the proto message can lead to huge memory consumption on devices as well as on the user's side.
 *
 * The logcat is read only once on the am instrument's side and the StringBuilder is uncapped. So if you have a huge logcat -
 * you'll have to transfer it via the socket and then also parse from protobuf and allocate a String in the JVM.
 *
 * The read loop catches exception and doesn't have a way to suspend until it can read the message properly.
 *
 * This needs work on the am instrument's side:
 * - proper framing support with length of the message sent first, then the actual message
 * - streaming logcat support: this should NOT be a one-shot operation, instead we can append to file while the test executes
 * - logcat command customisation: there is no way to change the format or filter at the moment, the command is hard-coded
 *
 * see frameworks/base/cmds/am/src/com/android/commands/am/Instrument.java#readLogcat
 */
class ProtoInstrumentationResponseTransformer(maxProtobufPacketLength: Long = Const.MAX_PROTOBUF_PACKET_LENGTH) :
    ProgressiveResponseTransformer<List<TestEvent>?> {
    private val backingFile = RandomAccessFile(File.createTempFile("tmp-proto", null, null), "rw")
    private val channel = backingFile.channel
    private val buffer: MappedByteBuffer =
        channel.map(FileChannel.MapMode.READ_WRITE, 0L, maxProtobufPacketLength).apply { compatLimit(0) }

    private var state: State = NotStarted
    private var testStatuses = linkedMapOf<TestIdentifier, TestStatusAggregator>()

    private val timeRegex = "Time: \\s*([\\d\\,]*[\\d\\.]+)".toRegex()

    override suspend fun process(bytes: ByteArray, offset: Int, limit: Int): List<TestEvent>? {
        if (state.terminal) return null

        buffer.compatLimit(buffer.limit() + limit)
        buffer.put(bytes, offset, limit)

        try {
            buffer.compatPosition(0)
            val session = InstrumentationData.Session.parseFrom(buffer)
            val events = mutableListOf<TestEvent>()
            for (status in session.testStatusList ?: emptyList()) {
                if (state == NotStarted) {
                    val testCount =
                        status.results?.entriesList?.filter { entry -> entry.key?.let { it == StatusKey.NUMTESTS.value } ?: false }
                            ?.mapNotNull { it.valueInt }
                            ?.firstOrNull()
                            ?: 0

                    if (testCount == 0) return null

                    state = Running
                    events.add(TestRunStartedEvent(testCount))
                }

                var testClassName = ""
                var testMethodName = ""
                var stackTrace = ""
                val testMetrics = mutableMapOf<String, String>()
                status.results?.entriesList?.forEach { entry ->
                    if (entry.key != null) {
                        when (StatusKey.of(entry.key)) {
                            StatusKey.TEST -> testMethodName = entry.valueString ?: testClassName
                            StatusKey.CLASS -> testClassName = entry.valueString ?: testMethodName
                            StatusKey.STACK -> stackTrace = entry.valueString ?: stackTrace
                            StatusKey.CURRENT -> Unit //Test index
                            StatusKey.NUMTESTS, StatusKey.ERROR, StatusKey.SHORTMSG, StatusKey.STREAM, StatusKey.ID -> Unit
                            StatusKey.UNKNOWN -> testMetrics[entry.key] = entry.valueToString()
                        }
                    }
                }

                var resultCodeOverride: Status? = null
                if (testClassName.isEmpty() && testMethodName.isEmpty()) {
                    testStatuses.entries.lastOrNull()?.let { previousTestStatus ->
                        testClassName = previousTestStatus.key.className
                        testMethodName = previousTestStatus.key.testName
                        resultCodeOverride = previousTestStatus.value.statusCode
                    } ?: run { testClassName = ""; testMethodName = "" }
                }

                if (testClassName.isNotBlank() && testMethodName.isNotBlank()) {
                    events.addAll(
                        updateTestState(
                            testClassName,
                            testMethodName,
                            resultCodeOverride ?: Status.valueOf(status.resultCode),
                            status.logcat,
                            stackTrace,
                            testMetrics
                        )
                    )
                }
            }

            if (session.hasSessionStatus()) {
                state = Finished

                val testRunMetrics = mutableMapOf<String, String>()
                when (session.sessionStatus.statusCode) {
                    InstrumentationData.SessionStatusCode.SESSION_FINISHED -> {
                        if ((session.sessionStatus.resultCode) == SessionResultCode.ERROR.value) {
                            val errorMessage = session.sessionStatus.results?.entriesList
                                ?.filter { entry ->
                                    entry.key?.let { it == StatusKey.SHORTMSG.value } ?: false
                                }
                                ?.mapNotNull { it.valueString }?.firstOrNull() ?: ""
                            events.add(TestRunFailed(errorMessage))
                        }

                        session.sessionStatus.results?.entriesList?.forEach { entry ->
                            if (entry.key != null && StatusKey.of(entry.key) == StatusKey.UNKNOWN) {
                                testRunMetrics[entry.key] = entry.valueToString()
                            }
                        }
                    }
                    InstrumentationData.SessionStatusCode.SESSION_ABORTED -> {
                        val errorText = session.sessionStatus.errorText ?: ""
                        val lastTest = testStatuses.entries.lastOrNull()

                        if (lastTest != null && !lastTest.value.statusCode.isTerminal()) {
                            events.add(TestFailed(lastTest.key, ""))

                            lastTest.value.metrics[Const.TEST_LOGCAT_METRIC] = lastTest.value.logcatBuilder.toString()
                            events.add(TestEnded(lastTest.key, lastTest.value.metrics))
                        }

                        events.add(TestRunFailed(errorText))
                    }
                    else -> Unit
                }

                val sessionOutput = session.sessionStatus.results?.entriesList
                    ?.filter { it.key == StatusKey.STREAM.value }
                    ?.mapNotNull { it.valueToString() }
                    ?.firstOrNull()

                if (sessionOutput != null) {
                    val matchResult = timeRegex.find(sessionOutput)
                    val elapsedTime = matchResult?.groups?.get(1)?.let { it.value.toFloatOrNull() }?.let { (it * 1000).toLong() } ?: 0L
                    events.add(TestRunEnded(elapsedTime, testRunMetrics))
                }
            }

            /**
             * Handling fragmentation
             * This will fail if the protobuf message contains the field multiple times and serializedSize is not equal to actual read size
             */
            val readSize = session.serializedSize
            val currentLimit = buffer.limit()
            buffer.compatPosition(readSize)
            buffer.compact()
            buffer.compatLimit(currentLimit - readSize)

            return events
        } catch (e: InvalidProtocolBufferException) {
            //wait for more input
            buffer.compatPosition(buffer.limit())
            return null
        }
    }

    override fun transform(): List<TestEvent>? {
        channel.close()
        backingFile.close()
        return null
    }

    private fun updateTestState(
        className: String,
        methodName: String,
        resultCode: Status,
        logcat: String?,
        stackTrace: String,
        testMetrics: Map<String, String>
    ): List<TestEvent> {
        val id = TestIdentifier(className, methodName)
        var events = mutableListOf<TestEvent>()
        val statusAggregator = testStatuses.computeIfAbsent(id) {
            events.add(TestStarted(id))
            return@computeIfAbsent TestStatusAggregator(Status.START)
        }

        logcat?.let { statusAggregator.logcatBuilder.append(it) }
        statusAggregator.metrics.putAll(testMetrics)

        if (statusAggregator.statusCode == resultCode) {
            return events
        }

        when (resultCode) {
            Status.ERROR, Status.FAILURE -> {
                events.add(TestFailed(id, stackTrace))
            }
            Status.IGNORED -> {
                events.add(TestIgnored(id))
                statusAggregator.logcatBuilder.clear()
            }
            Status.ASSUMPTION_FAILURE -> {
                events.add(TestAssumptionFailed(id, stackTrace))
            }
            Status.SUCCESS, Status.START, Status.IN_PROGRESS, Status.UNKNOWN -> Unit
        }

        if (resultCode.isTerminal()) {
            statusAggregator.metrics[Const.TEST_LOGCAT_METRIC] = statusAggregator.logcatBuilder.toString()
            events.add(TestEnded(id, statusAggregator.metrics))
        }

        return events
    }
}

private fun InstrumentationData.ResultsBundleEntry.valueToString(): String {
    return when {
        hasValueString() -> valueString
        hasValueInt() -> valueInt.toString()
        hasValueLong() -> valueLong.toString()
        hasValueFloat() -> valueFloat.toString()
        hasValueDouble() -> valueDouble.toString()
        hasValueBytes() -> valueBytes.toString()
        hasValueBundle() -> valueBundle.toString()
        else -> ""
    }
}
