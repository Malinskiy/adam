---
layout: default
title:  "Monitor devices"
parent: Async requests
nav_order: 3
---

When just listing devices once (`ListDevicesRequest`) is not enough, when you want to continually monitor if devices change their states
 (disconnect, connect, etc) please use the following request:

```kotlin
val deviceEventsChannel: ReceiveChannel<List<Device>> = adb.execute(
                    request = AsyncDeviceMonitorRequest(),
                    scope = GlobalScope
                )

while (!deviceEventsChannel.isClosedForReceive) {
    val currentDeviceList = deviceEventsChannel.receive()
    //...
}
```

Keep in mind that this will send the device events for all devices **even if some of them didn't change**.