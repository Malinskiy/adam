---
layout: default
title:  "Stream logcat"
parent: Async requests
nav_order: 4
---

## Stream logcat output

Recording the output from logcat (for example when writing to a file):

```kotlin
val channel = adb.execute(
    request = ChanneledLogcatRequest(),
    scope = GlobalScope,
    serial = "emulator-5554"
)

val line = channel.receive()
//write to a file...

//Dispose of channel to close the resources
channel.cancel()
```

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

If you just need to dump the log file consider using [`SyncLogcatRequest`]({% link _docs/sync-requests/read-logcat.md %})