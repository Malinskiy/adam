---
layout: default
title:  "File operations"
parent: Async requests
nav_order: 2
---

## Pull file
Use the following to pull a file with a known path on the device

```kotlin
val testFile = createTempFile()
val channel = adb.execute(
    request = PullFileRequest(
        remotePath = "/data/local/tmp/testfile", 
        local = testFile
    ),
    scope = GlobalScope,
    "emulator-5554")

var percentage = 0
while (!channel.isClosedForReceive) {
    val percentageDouble = channel.receiveOrNull() ?: break
}
```

Notice that the request streams progress while the operation is in progress.

If you don't know the path to remote file use `ListFilesRequest`.

## Push file
To push a local file to Android device's folder (`remotePath` should be the full path with the name of the target file):

```kotlin
val file = File("some-file")
val channel = server.execute(
    PushFileRequest(
        local = file, 
        remotePath = "/data/local/tmp/some-file",
        mode = "0644"
    ), 
    scope = GlobalScope, 
    serial = "emulator-5554"
)

while (!channel.isClosedForReceive) {
    channel.poll()
}
```

**mode** is the access rights in octal.