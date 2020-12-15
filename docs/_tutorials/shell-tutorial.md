---
layout: default
title:  "Execute shell request"
nav_order: 1
---

All the operations in adam require to be executed in some coroutine scope. For simplicity, you can run everything in `runBlocking{}` for
 trying out, but you should know/get to know coroutines and how to use them. In all the examples below the scoping will be omitted.

First, we need to make sure adb server is actually running:

```kotlin
StartAdbInteractor().execute()
```

Next, we create an instance of `AndroidDebugBridgeClient` using the factory:
```kotlin
val adb = AndroidDebugBridgeClientFactory().build()
```

The `AndroidDebugBridgeClient` instance `adb` has an `execute()` method to execute a request. Right now we don't know what devices are
 connected to a particular adb server. Let's list them and find one that we can use:
 
```kotlin
val devices: List<Device> = adbRule.adb.execute(ListDevicesRequest())
val device = devices.firstOrNull { it.state == DeviceState.DEVICE } ?: throw RuntimeException("no devices available")
```

Now we have a device and can execute a request for it:

```kotlin
val response: String = adbRule.adb.execute(ShellCommandRequest("echo hello"), device.serial)
```

All the waiting for response and establishing a transport connection happens transparently, you don't need to wait for anything. This also
 doesn't allocate new threads.