---
layout: default title: "adbd (on-device)"
nav_order: 11 has_toc: false permalink: /docs/adbd
---

## Root permissions

You can restart adbd with root permissions:

```kotlin
val stdout = adb.execute(
    request = RestartAdbdRequest(RootAdbdMode),
    serial = "emulator-5554"
)

println(stdout)
```

Or without root permissions:

```kotlin
val stdout = adb.execute(
    request = RestartAdbdRequest(UnrootAdbdMode),
    serial = "emulator-5554"
)

println(stdout)
```

## Switching transport

You can switch your device to start listening on a TCP port instead of USB:

```kotlin
val stdout = adb.execute(
    request = RestartAdbdRequest(TcpIpAdbdMode(8080)),
    serial = "emulator-5554"
)

println(stdout)
```

To switch back to USB:

```kotlin
val stdout = adb.execute(
    request = RestartAdbdRequest(UsbAdbdMode),
    serial = "emulator-5554"
)

println(stdout)
```