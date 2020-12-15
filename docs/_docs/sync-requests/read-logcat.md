---
layout: default
title:  "Read logcat"
parent: Sync requests
nav_order: 4
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
    modes: List<LogcatReadMode> = listOf(LogcatReadMode.long), //
    buffers: List<LogcatBuffer> = listOf(LogcatBuffer.default),
    pid: Long? = null,
    lastReboot: Boolean? = null,
    filters: List<LogcatFilterSpec> = emptyList()
)
```

See the [official docs](https://developer.android.com/studio/command-line/logcat) for more info on what these options change.

If you need to continuously stream logcat output see [`ChanneledLogcatRequest`]({% link _docs/async-requests/stream-logcat.md %})