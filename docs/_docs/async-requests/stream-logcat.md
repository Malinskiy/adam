---
layout: default
title:  "Stream logcat"
parent: Async requests
nav_order: 4
---

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
