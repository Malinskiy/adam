---
layout: default
title:  "3. Install package"
nav_order: 3
---

## 3. Install package

Here is the boilerplate from [part 1]({% link _tutorials/1-shell.md %}) to setup the communication with the device:
```kotlin
StartAdbInteractor().execute()
val adb = AndroidDebugBridgeClientFactory().build()
val devices: List<Device> = adb.execute(ListDevicesRequest())
val device = devices.firstOrNull { it.state == DeviceState.DEVICE } ?: throw RuntimeException("no devices available")
```

Now we need to execute the request. The `InstallRemotePackageRequest` installs a package from file that is already available on the device.
 This means that we first need to transfer our package to the device:
 
```kotlin
val apkFile = File("/my/precious/application/app-debug.apk")
val fileName = apkFile.name
val channel = adb.execute(PushFileRequest(testFile, "/data/local/tmp/$fileName"), GlobalScope, serial = device.serial)
while (!channel.isClosedForReceive) {
    val progress: Double? = channel.poll()
}
```
After executing the request we need to poll the channel for progress until the channel is closed.

Next we need to actually install this file:
```kotlin
val output: String = adb.execute(InstallRemotePackageRequest("/data/local/tmp/$fileName", true), serial = device.serial)
if(!output.startsWith("Success")) throw RuntimeException("Unable to install the apk")
```
If everything is ok then the output should contain something along the lines of `Success`.

Next we can verify that this package was indeed installed:
```kotlin
val packages: List<Package> = adb.execute(PmListRequest(), serial = device.serial)
val pkg: Package? = packages.find { it.name == "com.example" }
```
