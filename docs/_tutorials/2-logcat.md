---
layout: default
title:  "2. Stream logcat"
nav_order: 2
---

## 2. Streaming logcat output

Some operations in adam require you to stream the output. One such example is streaming the logcat since this source of data will not stop
 producing output unless you stop reading it or the device terminates.

Here is the boilerplate from [part 1]({% link _tutorials/1-shell.md %}) to setup the communication with the device:
```kotlin
StartAdbInteractor().execute()
val adb = AndroidDebugBridgeClientFactory().build()
val devices: List<Device> = adbRule.adb.execute(ListDevicesRequest())
val device = devices.firstOrNull { it.state == DeviceState.DEVICE } ?: throw RuntimeException("no devices available")
```

Now we need to execute the request:
```kotlin
val response: ReceiveChannel<String> = adb.execute(ChanneledLogcatRequest(), device.serial)
```

Pay attention to the return type `ReceiveChannel<String>`. This means that we might get more instances of `String` as the time goes by. In
 order to read the output we do the following:

```kotlin
do {
    val line = channel.receiveOrNull()?.let { println(it) }
    
    if(externalSignal) {
        channel.cancel()
        break
    }
} while (line != null)
```

First, we try to receive the output. This might succeed, then we print the string. This might fail, then we don't print anything.

Second, we check some external signal to stop streaming logcat (user pressed a key or something else). To close the whole request we need
 to cancel the channel. Then we break out of the loop.
 
Third, we want to continue this loop until we reach other the device failure to provide us the output or we receive some external signal to
stop.

There are many more options available for [`ChanneledLogcatRequest`]({% link _docs/logcat/logcat.md %}) that change the format of the output
as well as filtering and more.