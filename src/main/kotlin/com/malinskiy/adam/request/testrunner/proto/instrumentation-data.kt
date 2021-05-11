@file:OptIn(pbandk.PublicForGeneratedCode::class)

package com.malinskiy.adam.request.testrunner.proto

sealed class SessionStatusCode(override val value: Int, override val name: String? = null) : pbandk.Message.Enum {
    override fun equals(other: Any?) = other is SessionStatusCode && other.value == value
    override fun hashCode() = value.hashCode()
    override fun toString() = "SessionStatusCode.${name ?: "UNRECOGNIZED"}(value=$value)"

    object SESSION_FINISHED : SessionStatusCode(0, "SESSION_FINISHED")
    object SESSION_ABORTED : SessionStatusCode(1, "SESSION_ABORTED")
    class UNRECOGNIZED(value: Int) : SessionStatusCode(value)

    companion object : pbandk.Message.Enum.Companion<SessionStatusCode> {
        val values: List<SessionStatusCode> by lazy { listOf(SESSION_FINISHED, SESSION_ABORTED) }
        override fun fromValue(value: Int) = values.firstOrNull { it.value == value } ?: UNRECOGNIZED(value)
        override fun fromName(name: String) =
            values.firstOrNull { it.name == name } ?: throw IllegalArgumentException("No SessionStatusCode with name: $name")
    }
}

data class ResultsBundleEntry(
    val key: String? = null,
    val valueString: String? = null,
    val valueInt: Int? = null,
    val valueFloat: Float? = null,
    val valueDouble: Double? = null,
    val valueLong: Long? = null,
    val valueBundle: ResultsBundle? = null,
    val valueBytes: pbandk.ByteArr? = null,
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(other: pbandk.Message?) = protoMergeImpl(other)
    override val descriptor get() = Companion.descriptor
    override val protoSize by lazy { super.protoSize }

    companion object : pbandk.Message.Companion<ResultsBundleEntry> {
        val defaultInstance by lazy { ResultsBundleEntry() }
        override fun decodeWith(u: pbandk.MessageDecoder) = ResultsBundleEntry.decodeWithImpl(u)

        override val descriptor: pbandk.MessageDescriptor<ResultsBundleEntry> by lazy {
            val fieldsList = ArrayList<pbandk.FieldDescriptor<ResultsBundleEntry, *>>(8).apply {
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "key",
                        number = 1,
                        type = pbandk.FieldDescriptor.Type.Primitive.String(hasPresence = true),
                        jsonName = "key",
                        value = ResultsBundleEntry::key
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "value_string",
                        number = 2,
                        type = pbandk.FieldDescriptor.Type.Primitive.String(hasPresence = true),
                        jsonName = "valueString",
                        value = ResultsBundleEntry::valueString
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "value_int",
                        number = 3,
                        type = pbandk.FieldDescriptor.Type.Primitive.SInt32(hasPresence = true),
                        jsonName = "valueInt",
                        value = ResultsBundleEntry::valueInt
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "value_float",
                        number = 4,
                        type = pbandk.FieldDescriptor.Type.Primitive.Float(hasPresence = true),
                        jsonName = "valueFloat",
                        value = ResultsBundleEntry::valueFloat
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "value_double",
                        number = 5,
                        type = pbandk.FieldDescriptor.Type.Primitive.Double(hasPresence = true),
                        jsonName = "valueDouble",
                        value = ResultsBundleEntry::valueDouble
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "value_long",
                        number = 6,
                        type = pbandk.FieldDescriptor.Type.Primitive.SInt64(hasPresence = true),
                        jsonName = "valueLong",
                        value = ResultsBundleEntry::valueLong
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "value_bundle",
                        number = 7,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = ResultsBundle.Companion),
                        jsonName = "valueBundle",
                        value = ResultsBundleEntry::valueBundle
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "value_bytes",
                        number = 8,
                        type = pbandk.FieldDescriptor.Type.Primitive.Bytes(hasPresence = true),
                        jsonName = "valueBytes",
                        value = ResultsBundleEntry::valueBytes
                    )
                )
            }
            pbandk.MessageDescriptor(
                messageClass = ResultsBundleEntry::class,
                messageCompanion = this,
                fields = fieldsList
            )
        }
    }
}

data class ResultsBundle(
    val entries: List<ResultsBundleEntry> = emptyList(),
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(other: pbandk.Message?) = protoMergeImpl(other)
    override val descriptor get() = Companion.descriptor
    override val protoSize by lazy { super.protoSize }

    companion object : pbandk.Message.Companion<ResultsBundle> {
        val defaultInstance by lazy { ResultsBundle() }
        override fun decodeWith(u: pbandk.MessageDecoder) = ResultsBundle.decodeWithImpl(u)

        override val descriptor: pbandk.MessageDescriptor<ResultsBundle> by lazy {
            val fieldsList = ArrayList<pbandk.FieldDescriptor<ResultsBundle, *>>(1).apply {
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "entries",
                        number = 1,
                        type = pbandk.FieldDescriptor.Type.Repeated<ResultsBundleEntry>(
                            valueType = pbandk.FieldDescriptor.Type.Message(
                                messageCompanion = ResultsBundleEntry.Companion
                            )
                        ),
                        jsonName = "entries",
                        value = ResultsBundle::entries
                    )
                )
            }
            pbandk.MessageDescriptor(
                messageClass = ResultsBundle::class,
                messageCompanion = this,
                fields = fieldsList
            )
        }
    }
}

data class TestStatus(
    val resultCode: Int? = null,
    val results: ResultsBundle? = null,
    val logcat: String? = null,
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(other: pbandk.Message?) = protoMergeImpl(other)
    override val descriptor get() = Companion.descriptor
    override val protoSize by lazy { super.protoSize }

    companion object : pbandk.Message.Companion<TestStatus> {
        val defaultInstance by lazy { TestStatus() }
        override fun decodeWith(u: pbandk.MessageDecoder) = TestStatus.decodeWithImpl(u)

        override val descriptor: pbandk.MessageDescriptor<TestStatus> by lazy {
            val fieldsList = ArrayList<pbandk.FieldDescriptor<TestStatus, *>>(3).apply {
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "result_code",
                        number = 3,
                        type = pbandk.FieldDescriptor.Type.Primitive.SInt32(hasPresence = true),
                        jsonName = "resultCode",
                        value = TestStatus::resultCode
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "results",
                        number = 4,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = ResultsBundle.Companion),
                        jsonName = "results",
                        value = TestStatus::results
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "logcat",
                        number = 5,
                        type = pbandk.FieldDescriptor.Type.Primitive.String(hasPresence = true),
                        jsonName = "logcat",
                        value = TestStatus::logcat
                    )
                )
            }
            pbandk.MessageDescriptor(
                messageClass = TestStatus::class,
                messageCompanion = this,
                fields = fieldsList
            )
        }
    }
}

data class SessionStatus(
    val statusCode: SessionStatusCode? = null,
    val errorText: String? = null,
    val resultCode: Int? = null,
    val results: ResultsBundle? = null,
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(other: pbandk.Message?) = protoMergeImpl(other)
    override val descriptor get() = Companion.descriptor
    override val protoSize by lazy { super.protoSize }

    companion object : pbandk.Message.Companion<SessionStatus> {
        val defaultInstance by lazy { SessionStatus() }
        override fun decodeWith(u: pbandk.MessageDecoder) = SessionStatus.decodeWithImpl(u)

        override val descriptor: pbandk.MessageDescriptor<SessionStatus> by lazy {
            val fieldsList = ArrayList<pbandk.FieldDescriptor<SessionStatus, *>>(4).apply {
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "status_code",
                        number = 1,
                        type = pbandk.FieldDescriptor.Type.Enum(enumCompanion = SessionStatusCode.Companion, hasPresence = true),
                        jsonName = "statusCode",
                        value = SessionStatus::statusCode
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "error_text",
                        number = 2,
                        type = pbandk.FieldDescriptor.Type.Primitive.String(hasPresence = true),
                        jsonName = "errorText",
                        value = SessionStatus::errorText
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "result_code",
                        number = 3,
                        type = pbandk.FieldDescriptor.Type.Primitive.SInt32(hasPresence = true),
                        jsonName = "resultCode",
                        value = SessionStatus::resultCode
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "results",
                        number = 4,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = ResultsBundle.Companion),
                        jsonName = "results",
                        value = SessionStatus::results
                    )
                )
            }
            pbandk.MessageDescriptor(
                messageClass = SessionStatus::class,
                messageCompanion = this,
                fields = fieldsList
            )
        }
    }
}

data class Session(
    val testStatus: List<TestStatus> = emptyList(),
    val sessionStatus: SessionStatus? = null,
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(other: pbandk.Message?) = protoMergeImpl(other)
    override val descriptor get() = Companion.descriptor
    override val protoSize by lazy { super.protoSize }

    companion object : pbandk.Message.Companion<Session> {
        val defaultInstance by lazy { Session() }
        override fun decodeWith(u: pbandk.MessageDecoder) = Session.decodeWithImpl(u)

        override val descriptor: pbandk.MessageDescriptor<Session> by lazy {
            val fieldsList = ArrayList<pbandk.FieldDescriptor<Session, *>>(2).apply {
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "test_status",
                        number = 1,
                        type = pbandk.FieldDescriptor.Type.Repeated<TestStatus>(
                            valueType = pbandk.FieldDescriptor.Type.Message(
                                messageCompanion = TestStatus.Companion
                            )
                        ),
                        jsonName = "testStatus",
                        value = Session::testStatus
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "session_status",
                        number = 2,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = SessionStatus.Companion),
                        jsonName = "sessionStatus",
                        value = Session::sessionStatus
                    )
                )
            }
            pbandk.MessageDescriptor(
                messageClass = Session::class,
                messageCompanion = this,
                fields = fieldsList
            )
        }
    }
}

fun ResultsBundleEntry?.orDefault() = this ?: ResultsBundleEntry.defaultInstance

private fun ResultsBundleEntry.protoMergeImpl(plus: pbandk.Message?): ResultsBundleEntry = (plus as? ResultsBundleEntry)?.copy(
    key = plus.key ?: key,
    valueString = plus.valueString ?: valueString,
    valueInt = plus.valueInt ?: valueInt,
    valueFloat = plus.valueFloat ?: valueFloat,
    valueDouble = plus.valueDouble ?: valueDouble,
    valueLong = plus.valueLong ?: valueLong,
    valueBundle = valueBundle?.plus(plus.valueBundle) ?: plus.valueBundle,
    valueBytes = plus.valueBytes ?: valueBytes,
    unknownFields = unknownFields + plus.unknownFields
) ?: this

@Suppress("UNCHECKED_CAST")
private fun ResultsBundleEntry.Companion.decodeWithImpl(u: pbandk.MessageDecoder): ResultsBundleEntry {
    var key: String? = null
    var valueString: String? = null
    var valueInt: Int? = null
    var valueFloat: Float? = null
    var valueDouble: Double? = null
    var valueLong: Long? = null
    var valueBundle: ResultsBundle? = null
    var valueBytes: pbandk.ByteArr? = null

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> key = _fieldValue as String
            2 -> valueString = _fieldValue as String
            3 -> valueInt = _fieldValue as Int
            4 -> valueFloat = _fieldValue as Float
            5 -> valueDouble = _fieldValue as Double
            6 -> valueLong = _fieldValue as Long
            7 -> valueBundle = _fieldValue as ResultsBundle
            8 -> valueBytes = _fieldValue as pbandk.ByteArr
        }
    }
    return ResultsBundleEntry(
        key, valueString, valueInt, valueFloat,
        valueDouble, valueLong, valueBundle, valueBytes, unknownFields
    )
}

fun ResultsBundle?.orDefault() = this ?: ResultsBundle.defaultInstance

private fun ResultsBundle.protoMergeImpl(plus: pbandk.Message?): ResultsBundle = (plus as? ResultsBundle)?.copy(
    entries = entries + plus.entries,
    unknownFields = unknownFields + plus.unknownFields
) ?: this

@Suppress("UNCHECKED_CAST")
private fun ResultsBundle.Companion.decodeWithImpl(u: pbandk.MessageDecoder): ResultsBundle {
    var entries: pbandk.ListWithSize.Builder<ResultsBundleEntry>? = null

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> entries = (entries ?: pbandk.ListWithSize.Builder()).apply { this += _fieldValue as Sequence<ResultsBundleEntry> }
        }
    }
    return ResultsBundle(pbandk.ListWithSize.Builder.fixed(entries), unknownFields)
}

fun TestStatus?.orDefault() = this ?: TestStatus.defaultInstance

private fun TestStatus.protoMergeImpl(plus: pbandk.Message?): TestStatus = (plus as? TestStatus)?.copy(
    resultCode = plus.resultCode ?: resultCode,
    results = results?.plus(plus.results) ?: plus.results,
    logcat = plus.logcat ?: logcat,
    unknownFields = unknownFields + plus.unknownFields
) ?: this

@Suppress("UNCHECKED_CAST")
private fun TestStatus.Companion.decodeWithImpl(u: pbandk.MessageDecoder): TestStatus {
    var resultCode: Int? = null
    var results: ResultsBundle? = null
    var logcat: String? = null

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            3 -> resultCode = _fieldValue as Int
            4 -> results = _fieldValue as ResultsBundle
            5 -> logcat = _fieldValue as String
        }
    }
    return TestStatus(resultCode, results, logcat, unknownFields)
}

fun SessionStatus?.orDefault() = this ?: SessionStatus.defaultInstance

private fun SessionStatus.protoMergeImpl(plus: pbandk.Message?): SessionStatus = (plus as? SessionStatus)?.copy(
    statusCode = plus.statusCode ?: statusCode,
    errorText = plus.errorText ?: errorText,
    resultCode = plus.resultCode ?: resultCode,
    results = results?.plus(plus.results) ?: plus.results,
    unknownFields = unknownFields + plus.unknownFields
) ?: this

@Suppress("UNCHECKED_CAST")
private fun SessionStatus.Companion.decodeWithImpl(u: pbandk.MessageDecoder): SessionStatus {
    var statusCode: SessionStatusCode? = null
    var errorText: String? = null
    var resultCode: Int? = null
    var results: ResultsBundle? = null

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> statusCode = _fieldValue as SessionStatusCode
            2 -> errorText = _fieldValue as String
            3 -> resultCode = _fieldValue as Int
            4 -> results = _fieldValue as ResultsBundle
        }
    }
    return SessionStatus(statusCode, errorText, resultCode, results, unknownFields)
}

fun Session?.orDefault() = this ?: Session.defaultInstance

private fun Session.protoMergeImpl(plus: pbandk.Message?): Session = (plus as? Session)?.copy(
    testStatus = testStatus + plus.testStatus,
    sessionStatus = sessionStatus?.plus(plus.sessionStatus) ?: plus.sessionStatus,
    unknownFields = unknownFields + plus.unknownFields
) ?: this

@Suppress("UNCHECKED_CAST")
private fun Session.Companion.decodeWithImpl(u: pbandk.MessageDecoder): Session {
    var testStatus: pbandk.ListWithSize.Builder<TestStatus>? = null
    var sessionStatus: SessionStatus? = null

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> testStatus = (testStatus ?: pbandk.ListWithSize.Builder()).apply { this += _fieldValue as Sequence<TestStatus> }
            2 -> sessionStatus = _fieldValue as SessionStatus
        }
    }
    return Session(pbandk.ListWithSize.Builder.fixed(testStatus), sessionStatus, unknownFields)
}
