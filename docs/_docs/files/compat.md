---
layout: default
title:  "Compatibility mode"
parent: Files
nav_order: 1
---

## Stat file

Optionally uses Feature.STAT_V2 {: .label .label-blue } The following request will return file stats:

```kotlin
val stats = adb.execute(CompatStatFileRequest("/data/local/tmp/app-debug.apk", supportedFeaturesList), "emulator-5554")
```

The model of stats is represented as `FileEntry` that will be an instance of `FileEntryV1` or `FileEntryV2` depending on the features
available:

```kotlin
sealed class FileEntry {
    abstract val mode: UInt
    abstract val name: String?
    abstract val mtime: Instant

    fun isDirectory()
    fun isRegularFile()
    fun isBlockDevice()
    fun isCharDevice()
    fun isLink()

    fun size()

    abstract fun exists(): Boolean
}
```

Name is optional and is only filled by list requests but not stat requests.

## List files

Optionally uses Feature.LS_V2 {: .label .label-blue } The following request willCpm return list of files for a particular path:

```kotlin
val list: List<FileEntry> = adb.execute(CompatListFileRequest("/sdcard/", supportedFeaturesList), "emulator-5554")
```

## Pull file

Optionally uses Feature.SENDRECV_V2 {: .label .label-blue } Use the following to pull a file(not a folder) with a known path on the device

```kotlin
launch {
    val channel = adb.execute(
        PullFileRequest("/data/local/tmp/testfile", createTempFile(), supportedFeaturesList, coroutineContext = coroutineContext),
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

Optionally uses Feature.SENDRECV_V2 {: .label .label-blue } To push a local file to Android device's folder (`remotePath` should be the full
path with the name of the target file):

```kotlin
launch {
    val file = File("some-file")
    val channel = adb.execute(
        PushFileRequest(
            local = file,
            remotePath = "/data/local/tmp/some-file",
            supportedFeaturesList,
            mode = "0644"
        ),
        scope = this,
        serial = "emulator-5554"
    )

    var percentage = 0
    for (percentageDouble in channel) {
        percentage = (percentageDouble * 100).roundToInt()
        println(percentage)
    }
}
```

**mode** is the access rights in octal represented as an instance of `String`.