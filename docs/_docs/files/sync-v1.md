---
layout: default title:  "sync v1"
parent: Files nav_order: 2
---

## Stat file

The following request will return file stats:

```kotlin
val stats = adb.execute(StatFileRequest("/data/local/tmp/app-debug.apk"), "emulator-5554")
```

The model of stats is represented as `FileEntryV1`:

```kotlin
data class FileEntryV1(
    val name: String? = null,
    val mode: UInt,
    val size: UInt,
    val mtime: Instant
) {
    fun exists(): Boolean
    fun isDirectory(): Boolean
    fun isRegularFile(): Boolean
    fun isBlockDevice(): Boolean
    fun isCharDevice(): Boolean
    fun isLink(): Boolean
}
```

Name is optional and is only filled by list requests but not stat requests.

## List files

The following request will return list of files for a particular path:

```kotlin
val list: List<FileEntryV1> = adb.execute(ListFileRequest("/sdcard/"), "emulator-5554")
```

## Pull file

Use the following to pull a file(not a folder) with a known path on the device

```kotlin
launch {
    val channel = adb.execute(
        PullFileRequest("/data/local/tmp/testfile", createTempFile(), coroutineContext = coroutineContext),
        scope = this,
        "emulator-5554"
    )

    var percentage = 0
    for (percentageDouble in channel) {
        percentage = (percentageDouble * 100).roundToInt()
        println(percentage)
    }
}
```

## Push file
To push a local file to Android device's folder (`remotePath` should be the full path with the name of the target file):

```kotlin
launch {
    val file = File("some-file")
    val channel = adb.execute(
        PushFileRequest(
            local = file,
            remotePath = "/data/local/tmp/some-file",
            mode = "0644"
        ),
        scope = this,
        serial = "emulator-5554"
    )
    
    var percentage = 0
    for(percentageDouble in channel) {
        percentage = (percentageDouble * 100).roundToInt()
        println(percentage)
    }
}
```

**mode** is the access rights in octal represented as an instance of `String`.