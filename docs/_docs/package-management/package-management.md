---
layout: default
title:  "Package management"
has_children: true
nav_order: 2
---

1. TOC
{:toc}

## Install a package

### Default installation mode

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

### Streaming installation
<div markdown="1">
Requires Feature.CMD or Feature.ABB_EXEC 
{: .label .label-yellow } 
Optionally uses Feature.APEX 
{: .label .label-blue }
</div>

This mode streams the package file so that you don't need to push the file to the device beforehand. This saves you a couple of requests,
 namely push file and delete file at the end.

```kotlin
val success = client.execute(
    StreamingPackageInstallRequest(
        pkg = testFile,
        supportedFeatures,
        reinstall = false,
        extraArgs = emptyList()
    ),
    serial = "emulator-5554"
)
```

### Split apk

Optionally uses Feature.CMD or Feature.ABB_EXEC 
{: .label .label-blue }

Install an apk split as follows:

```kotlin
val success = client.execute(
    InstallSplitPackageRequest(
        pkg = ApkSplitInstallationPackage(appFile1, appFile2),
        supportedFeatures,
        reinstall = false,
        extraArgs = emptyList()
    ),
    serial = "emulator-5554"
)
```

If both `CMD` and `ABB_EXEC` features are missing then falls back to 'exec:'. In this case there is no guarantee that the `pm` binary can
install the split packages at all.

### Atomic multi-package install
<div markdown="1">
Requires Feature.CMD or Feature.ABB_EXEC 
{: .label .label-yellow } 

Optionally uses Feature.APEX 
{: .label .label-blue }
</div>

This request installs multiple packages as a single atomic operation. If one of them fails - all will fail.

```kotlin
val success = client.execute(
    AtomicInstallPackageRequest(
        pkgList = listOf(
            SingleFileInstallationPackage(appFile),
            ApkSplitInstallationPackage(appFile1, appFile2)
        ),
        supportedFeatures,
        reinstall = false,
        extraArgs = emptyList()
    ),
    serial = "emulator-5554"
)
```

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
