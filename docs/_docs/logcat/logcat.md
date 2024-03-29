---
layout: default
title:  "Logcat"
nav_order: 8
---

## Retrieve logcat log

To read logcat once you can execute:

```kotlin
val log = adb.execute(
    request = SyncLogcatRequest(
        since = Instant.now().minusSeconds(60),
        filters = listOf(LogcatFilterSpec("TAG", LogcatVerbosityLevel.E))
    ),
    serial = "emulator-5554"
)
```

`SyncLogcatRequest` maps most of the options exposed by the underlying `logcat` command:

```kotlin
class SyncLogcatRequest(
    since: Instant? = null,
    modes: List<LogcatReadMode> = listOf(LogcatReadMode.long),
    buffers: List<LogcatBuffer> = listOf(LogcatBuffer.default),
    pid: Long? = null,
    lastReboot: Boolean? = null,
    filters: List<LogcatFilterSpec> = emptyList()
)
```

## Stream logcat output

Recording the output from logcat (for example when writing to a file):

```kotlin
launch {
    val channel = adb.execute(
        request = ChanneledLogcatRequest(),
        scope = this,
        serial = "emulator-5554"
    )

    val logcatChunk = channel.receive()
    //logcatChunk == "I/ActivityManager(  585): Starting activity: Intent { action=android.intent.action...}\nI/MyActivity( 1557): MyClass"
    //write to a file or append to a buffer

    //Dispose of channel to close the resources
    channel.cancel()
}
```

Logcat chunks that you receive might not be `\n` terminated so if you need to parse logcat output line-by-line then you need to accumulate
the chunks in a buffer first.

`ChanneledLogcatRequest` maps most of the options exposed by the underlying `logcat` command:

```kotlin
class ChanneledLogcatRequest(
    since: Instant? = null,
    modes: List<LogcatReadMode> = listOf(LogcatReadMode.long),
    buffers: List<LogcatBuffer> = emptyList(),
    pid: Long? = null,
    lastReboot: Boolean? = null,
    filters: List<LogcatFilterSpec> = emptyList()
)
```

See the [official docs](https://developer.android.com/studio/command-line/logcat) for more info on what these options change.
