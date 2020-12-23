---
layout: default
title:  "Caveats"
nav_order: 3
---
# Caveats
{: .no_toc }
When working with adam it's a good idea to keep the following things in mind.

1. TOC
{:toc}

## Response types

Every request in adam requires you to create an instance of `AndroidDebugBridgeClient` in order to execute a requests. All the requests
 produce either a **single response** (e.g. `ListDevicesRequest`):
 
```kotlin
val devices: List<Device> = adbClient.execute(request = ListDevicesRequest())
``` 

or request produces a **stream of responses**, e.g. a progress of pulling a file:

```kotlin
val testFile = createTempFile()
val channel = adbClient.execute(
    request = PullFileRequest("/data/local/tmp/testfile", testFile),
    scope = GlobalScope,
    serial = "emulator-5554"
)

var percentage = 0
while (!channel.isClosedForReceive) {
    val progressDouble = channel.receiveOrNull() ?: break
    println(progressDouble)
}
println("done!")
```

## Exception handling
In general, you can expect the following for any request:

1. `ClosedWriteChannelException` if the device connection is not be available anymore
2. `RequestRejectedException` if ADB server doesn't respond properly
3. `RequestValidationException` if request's `#validate()` returns false before execution

There are additional exceptions, namely:
* `PullFailedException`, `PushFailedException` and `UnsupportedSyncProtocolException` for file requests
* `UnsupportedForwardingSpecException` for port forwarding requests
* `UnsupportedImageProtocolException` for screenshot requests


## Request target
When executing the request agains an ADB server client sends what is the target for that particular request.

Possible targets are:
1. `HostTarget`.  When asking for information related to a device, 'host:' can also be interpreted as 'any single device or emulator
 connected to/running on the host'.
2. `SerialTarget`. This is a special form of query, where the 'host-serial:<serial-number>:' prefix can be used to indicate that the client
 is asking the ADB server for information related to a specific device.
3. `UsbTarget`. A variant of host-serial used to target the single USB device connected to the host. This will fail if there is none or more
 than one.
4. `LocalTarget`. A variant of host-serial used to target the single emulator instance running on the host. This will fail if there is none
 or more than one.
5. `NonSpecifiedTarget`

In most of the cases you _can_ specify any of them and there are sensible defaults. For example, `KillAdbRequest`'s default target is
 `HostTarget` since this request doesn't make sense for Android device itself.

For all the requests targeting a particular device, e.g. `ScreenCaptureRequest` you have to specify the `serial` parameter when executing,
 e.g.:
 
```kotlin
adb.execute(
            request = ScreenCaptureRequest(),
            serial = "emulator-5554"
            )
```

The serial for each particular device can be retrieved by executing either `ListDevicesRequest` or `AsyncDeviceMonitorRequest` 
