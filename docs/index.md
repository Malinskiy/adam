---
layout: default
title: Home
nav_order: 1
permalink: /
---

# Adam
Android Debug Bridge helper written in Kotlin

[Get started now](#getting-started){: .btn .btn-primary .fs-5 .mb-4 .mb-md-0 .mr-2 } [View it on GitHub](https://github.com/Malinskiy/adam){: .btn .fs-5 .mb-4 .mb-md-0 }

## Motivation
The only way to get access to the adb programmatically from java world currently is to use the ddmlib java project. Unfortunately it has several limitations, namely:

1. Sub-optimal resources usage
2. Code is not tested properly
3. Limitations of adb server are propagated to the user of ddmlib

To optimize the resources usage adam uses coroutines instead of blocking threads. This reduced the load dramatically for scenarios where dozens of devices are connected and are communicated with.
Full E2E testing with at least Android emulator is also used to guarantee stability.

## Supported functionality
* Shell
  * Basic `shell:` support (with stdout and patched exit code)
  * shell_v2 support (with separated stdout, stderr and exit code as well as stdin)
  * Exec shell with stdin on legacy devices without shell_v2 support
* Package install, uninstall, list
  * Streaming installation
  * Atomic multi-package installation
  * Apk split installation
  * Supports APEX
  * Sideload (with pre-KitKat support)
  * Install sessions support
* Device management
  * List connected devices
  * Monitor connected devices continuously
  * Fetch device features
  * Connect/disconnect/reconnect device
  * adb over WiFi pairing setup
  * Reboot device
* Files
  * List file using `ls`
  * Push/pull files and folders(recursive)
  * Stat, list, pull and push using `sync:`
  * Support for stat_v2, sendrecv_v2, ls_v2
* Emulator commands (`gsm call`, `rotate`, etc)
* Props
  * Get single prop
  * Get all props
* Instrumented tests
  * Raw output parsing
  * Proto output parsing
* Screen capture
  * Dynamic adapters with raw buffer and fast BufferedImage conversion
  * Supports legacy devices as well as new sRGB and DCI-P3 ones
* Logcat
  * Fetch logcat log
  * Monitor logcat continuously
* Port-forwarding (including reverse port-forwarding)
  * List ports
  * Add rule
  * Remove rule
  * Remove all rules
* Android Binder Bridge: "abb" and "abb_exec"
* Restart adbd on device: "root:", "unroot:", as well as switching transport "usb:", "tcpip:"
* Miscellaneous
  * Fetch adb server version
  * Kill adb server
  * Remount partition
  * Enable/disable dm-verity checking on userdebug builds
  * Fetch host features
  * Check if mDNS discovery is available
  * List all mDNS discovered services

Not to mention any device shell commands.

# Getting started

![Maven Central](https://img.shields.io/maven-central/v/com.malinskiy/adam)

To add a dependency on Adam using Maven, use the following:

```xml

<dependency>
  <groupId>com.malinskiy</groupId>
  <artifactId>adam</artifactId>
  <version>X.X.X</version>
</dependency>
```

To add a dependency using Gradle:

```
dependencies {
  implementation 'com.malinskiy.adam:adam:X.X.X'
}
```

# Basic usage example

```kotlin
//Start the adb server
StartAdbInteractor().execute()

//Create adb client
val adb = AndroidDebugBridgeClientFactory().build()

//Execute a request
val output = adb.execute(ShellCommandRequest("echo hello"), "emulator-5554")
println(output) // hello
```

## About the project

Adam is &copy; 2019-{{ "now" | date: "%Y" }} by [Anton Malinskiy](http://github.com/Malinskiy).

### License

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
       http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
    
### Contributing

When contributing to this repository, please first discuss the change you wish to make via issue,
email, or [Slack #adam](https://bit.ly/2LLghaW) with the owners of this repository before making a change.
