---
layout: default
title:  "Shell"
parent: Sync requests
nav_order: 1
---

You can execute arbitrary commands (`ls`, `date`, etc) on the device using the `ShellCommandRequest`:

```kotlin
val response: String =  adb.execute(
                request = ShellCommandRequest("echo hello"),
                serial = "emulator-5554"
            )
```

This request expects that the command immediately returns. If the command is expected to stream the output please use
 `ChanneledShellCommandRequest`.