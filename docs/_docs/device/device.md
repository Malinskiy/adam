---
layout: default
title: "Device management"
nav_order: 3
has_toc: false
permalink: /docs/monitor-devices
---

## List devices

This request will capture a snapshot of device states at a point of execution:

```kotlin
val devices: List<Device> = adb.execute(request = ListDevicesRequest())
```

## Monitoring device changes

If listing devices once is not enough, i.e. you want to continually monitor if devices change their states (disconnect, connect, etc)
use the following request:

```kotlin
val deviceEventsChannel: ReceiveChannel<List<Device>> = adb.execute(
    request = AsyncDeviceMonitorRequest(),
    scope = GlobalScope
)

for (currentDeviceList in deviceEventsChannel) {
    //...
}
```

Keep in mind that this will send the device events for all devices **even if some of them didn't change**.

## Fetch device features

This request will retrieve a list of features supported by a particular device:

```kotlin
val features: List<Feature> = adb.execute(request = FetchDeviceFeaturesRequest("emulator-5554"))
```

Here is a list of features adam is aware of:

* Feature.SHELL_V2: support for separate stdout, stderr and exit code
* Feature.CMD: The 'cmd' command is available, Android 24+
* Feature.STAT_V2: device supports extended FileEntryV2 format for stat operation
* Feature.LS_V2: device supports extended FileEntryV2 format for list operation
* Feature.APEX: adbd supports installing .apex packages
* Feature.ABB: adbd supports android binder bridge (abb) in interactive mode using shell protocol
* Feature.ABB_EXEC: adbd supports abb using raw pipe
* Feature.SENDRECV_V2: adbd supports version 2 of send/recv

There are more features, but adam is not using them at the moment.

Every time you see in the documentation something like Requires Feature.ABB {: .label .label-yellow }

it means that this request will not succeed unless the device has support for a particular feature. You can check the support by executing
the `FetchDeviceFeaturesRequest` beforehand or catch the `RequestValidationException`.

Sometimes a feature might be optionally used if there is a fallback, see docs for a particular request.

## Connect device

If you need to connect remote Android devices to a local adb server:

```kotlin
val output = adb.execute(ConnectDeviceRequest("10.0.0.2", 5555))
```

## Disconnect device

To disconnect a previously connected Android device:

```kotlin
val output = adb.execute(DisconnectDeviceRequest("10.0.0.2", 5555))
```

## Reconnect device

This request is quite tricky to use since the target of the request varies with the reconnection target

If you don't specify anything in reconnectTarget then it's treated as `find the first available device` and reconnect

```kotlin
val output = adb.execute(ReconnectRequest())
```

If you specify Device target then you have to provide the target either here or via serial during execution

```kotlin
val output = adb.execute(ReconnectRequest(reconnectTarget = Device, target = SerialTarget("10.0.0.2:5555")))
```

If you use Offline then you have to use the host target only

```kotlin
val output = adb.execute(ReconnectRequest(reconnectTarget = Offline, target = HostTarget))
```

## Pair device

Pairs adb server with device over WiFi connection
See https://developer.android.com/studio/command-line/adb#connect-to-a-device-over-wi-fi-android-11+

```kotlin
val output = adb.execute(PairDeviceRequest("10.0.0.2:39567", "123456"))
```

The target device should be in the form of `host[:port]`, port is optional.

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
