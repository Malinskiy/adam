![Maven Central](https://img.shields.io/maven-central/v/com.malinskiy/adam)
![Codecov](https://img.shields.io/codecov/c/github/Malinskiy/adam)

# adam
Android Debug Bridge helper written in Kotlin

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

License
-------

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
