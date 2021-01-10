---
layout: default title:  "Recommended mode"
parent: Files nav_order: 1
---

## Pull file(s)/folder(s)

Optionally uses Feature.STAT_V2, Feature.LS_V2, Feature.SENDRECV_V2 {: .label .label-blue } The following request will pull remote file(s)
or folder(s):

```kotlin
val success: Boolean = adb.execute(PullRequest("/data/local/tmp/testdir", destination, supportedFeatures), "emulator-5554")
```

Please note that this request doesn't handle file links. If source is a directory and the destination is an existing directory, a
subdirectory will be created

## Push file(s)/folder(s)

Optionally uses Feature.STAT_V2, Feature.LS_V2, Feature.SENDRECV_V2 {: .label .label-blue } The following request will push local file(s) or
folder(s) to the remote device:

```kotlin
val success: Boolean = adb.execute(PushRequest(source, "/data/local/tmp/testdir", supportedFeatures), "emulator-5554")
```

Please note that this request doesn't handle file links. If source is a directory and the destination is an existing directory, a
subdirectory will be created
