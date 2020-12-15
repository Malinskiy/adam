---
layout: default
title:  "Reboot device"
parent: Sync requests
nav_order: 7
---

## Reboot device

If you need to reboot a particular device (for example if it stopped executing requests properly):
```kotlin
adb.execute(request = RebootRequest(), serial = "emulator-5554")
```

Or if you want to reboot to recovery:
```kotlin
adb.execute(request = RebootRequest(mode = RECOVERY), serial = "emulator-5554")
```

Or bootloader:
```kotlin
adb.execute(request = RebootRequest(mode = BOOTLOADER), serial = "emulator-5554")
```
