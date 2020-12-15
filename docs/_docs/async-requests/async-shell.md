---
layout: default
title:  "Shell"
parent: Async requests
nav_order: 2
---

You can execute arbitrary commands (`cat`, `tail -f`, etc) on the device using the `ChanneledShellCommandRequest`:

```kotlin
val updates = adb.execute(
                    request = ChanneledShellCommandRequest("logcat -v"),
                    scope = GlobalScope,
                    serial = "emulator-5554"
                )

while (!updates.isClosedForReceive) {
    updates.receiveOrNull()?.let { println(it) }
}
```

This request expects that the command will stream the output. If the command is expected to immediately return the output please use
 `ShellCommandRequest`.
 