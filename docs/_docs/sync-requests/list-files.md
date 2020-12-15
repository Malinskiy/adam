---
layout: default
title:  "List files in directory"
parent: Sync requests
nav_order: 5
---

Traversing directories can be done using the following wrapper around `ls`:

```kotlin
val files: List<AndroidFile> = adb.execute(
    request = ListFilesRequest(
        directory = "/sdcard/"
    ),
    serial = "emulator-5554" 
)
```

`AndroidFile` is a data class with the following properties:

```kotlin
/**
 * @property permissions full permissions string, e.g. -rw-rw----
 * @property owner file owner, e.g. root
 * @property group file group, e.g. sdcard_rw
 * @property date e.g. 2020-12-01
 * @property time e.g. 22:22
 * @property name the file name without path, e.g. testfile.txt
 * @property directionality file's directory, e.g. /sdcard/
 * @property size file's size, e.g. 1024
 * @property type file's type
 * @property link if the file is a symbolic link, this field is what the link points to
 */
data class AndroidFile(
    val permissions: String,
    val owner: String,
    val group: String,
    val date: String,
    val time: String,
    val name: String,
    val directory: String,
    val size: Long,
    val type: AndroidFileType,
    val link: String? = null
)
```
