---
layout: default
title:  "Package management"
parent: Sync requests
nav_order: 2
---

## List installed packages
To list all installed packages:

```kotlin
val packages: List<Package> = adb.execute(
    request = PmListRequest(
        includePath = false
    ),
    serial = "emulator-5554" 
)
```

## Install a package
In order to install a package you first need to push the file with `PushFileRequest` to appropriate location. You should push your apks to
 `/data/local/tmp` since it's a user-writable path on all versions of Android (_so far_).

```kotlin
val output: String = adb.execute(
    request = InstallRemotePackageRequest(
        absoluteRemoteFilePath = "/data/local/tmp/$apkFileName", 
        reinstall = true,
        extraArgs = emptyList()
    ),
    serial = "emulator-5554"
)
```

## Uninstall package

```kotlin
val output: String = adb.execute(
    request = UninstallRemotePackageRequest(
        packageName = "com.example",
        keepData = false
    ), 
    serial = "emulator-5554"
)
```