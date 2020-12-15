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
* Package install, uninstall, list
* Logcat
* Props
* Instrumented tests
* Port forwarding
* Screen capture
* File push, pull, stat
* List connected devices (including continuous monitoring)
* Reboot

**+** any device shell commands (including continuous streaming output)

# Getting started
![Maven Central](https://img.shields.io/maven-central/v/com.malinskiy/adam)

To add a dependency on Adam using Maven, use the following:

```xml
<dependency>
  <groupId>com.malinskiy.marathon</groupId>
  <artifactId>adam</artifactId>
  <version>X.X.X</version>
</dependency>
```

To add a dependency using Gradle:

```
dependencies {
  implementation 'com.malinskiy.marathon:adam:X.X.X'
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
 