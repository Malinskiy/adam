---
layout: default
title: Miscellaneous
nav_order: 12
---

## Get adb server version

This request returns the adb server version specified in `adb/adb.h`
(e.g. [here](https://android.googlesource.com/platform/system/core/+/dd7bc3319deb2b77c5d07a51b7d6cd7e11b5beb0/adb/adb.h#36)). It is useful
for debugging incompatible versions of adb and also making sure your requests are supported by the adb server.

```kotlin
val version: Int = adb.execute(request = GetAdbServerVersionRequest())
```

## Kill adb server

This request is equivalent to executing `adb kill-server`:

```kotlin
adb.execute(request = KillAdbRequest())
```

## Remount partition

Remount partitions read-write. If a reboot is required, `autoReboot = true` will automatically reboot the device.

```kotlin
val output: String = adb.execute(request = RemountPartitionsRequest(autoReboot = false), "emulator-5554")
```

## Enable/disable dm-verity checking on userdebug builds

```kotlin
val output: String = adb.execute(request = SetDmVerityCheckingRequest(false), "emulator-5554")
```

## Fetch host features

```kotlin
val features: List<Feature> = adb.execute(request = FetchHostFeaturesRequest())
```

## Check if mDNS discovery is available
mDNS is used for automatic discovery and connection of remote devices (for example with Android 11 ADB over WiFi)

```kotlin
val status: MdnsStatus = adb.execute(MdnsCheckRequest())
```

## List all mDNS discovered services

```kotlin
val services: List<MdnsService> = adb.execute(ListMdnsServicesRequest())
```

```kotlin
data class MdnsService(
    val name: String,
    val serviceType: String,
    val url: String
)
```
