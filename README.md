![Maven Central](https://img.shields.io/maven-central/v/com.malinskiy/adam)
![Codecov](https://img.shields.io/codecov/c/github/Malinskiy/adam)
![Documentation](https://img.shields.io/badge/docs-documentation-green?link=https://malinskiy.github.io/adam/)

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
