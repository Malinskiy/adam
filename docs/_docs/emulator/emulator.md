---
layout: default title: "Emulator console"
nav_order: 4 has_toc: false permalink: /docs/emu
---

## Run emulator console command

This request will execute an emulator console command:

```kotlin
val devices: List<Device> = adb.execute(
    request =
    EmulatorCommandRequest(
        "help",
        InetSocketAddress("localhost", 5556)
    )
)
```

This request is completely different to other requests: it connects directly to emulator instead of adb server. For simplicity, it can be
used in the same way as adb server requests and shares the socket creation logic with other requests.

Every emulator exposes a port which you can find in the serial number (e.g. `emulator-5556`). This port is listening on loopback only by
default.
