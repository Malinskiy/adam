---
layout: default
title:  "Android JUnit 4 rules"
nav_order: 1
---

Adam supports tests that need to interact with the device be it adb or emulator console/gRPC access. This means that you can execute a test,
set the location on the device or emulate a particular sensor input.

This ability makes it possible to provide end-to-end testing environment for a lot of complex applications. Some examples could be a fitness
tracker app that needs to recognize a particular pattern of movement in the device, or an application that needs to handle phone calls/SMS.

Here is a list of currently supported sensors by the [emulator_controller.proto][1] just as an example of the possibilities:

```
  enum SensorType {
    // Measures the acceleration force in m/s2 that is applied to a device
    // on all three physical axes (x, y, and z), including the force of
    // gravity.
    ACCELERATION = 0;
    // Measures a device's rate of rotation in rad/s around each of the
    // three physical axes (x, y, and z).
    GYROSCOPE = 1;
    // Measures the ambient geomagnetic field for all three physical axes
    // (x, y, z) in μT.
    MAGNETIC_FIELD = 2;
    // Measures degrees of rotation that a device makes around all three
    // physical axes (x, y, z)
    ORIENTATION = 3;
    // Measures the temperature of the device in degrees Celsius (°C).
    TEMPERATURE = 4;
    // Measures the proximity of an object in cm relative to the view screen
    // of a device. This sensor is typically used to determine whether a
    // handset is being held up to a person's ear.
    PROXIMITY = 5;
    // Measures the ambient light level (illumination) in lx.
    LIGHT = 6;
    // Measures the ambient air pressure in hPa or mbar.
    PRESSURE = 7;
    // Measures the relative ambient humidity in percent (%).
    HUMIDITY = 8;
    MAGNETIC_FIELD_UNCALIBRATED = 9;
  }
```

In order to use the following rules, support from your test runner is required. A reference implementation of this can be found
in [Marathon](https://github.com/MarathonLabs/marathon) test runner.

Tests can be injected with a usable implementation of adb, console or gRPC as following using a custom JUnit 4 rule. All rules provide ways
to fail via assumption, e.g. `EmulatorGrpcRule(mode = Mode.ASSUME)` in case testing is done in a mixed device environment where not every
run has emulators/adb access.

## AdbRule

The rule for adb provides access to the usual adam requests that target only the current device via explicitly specifying the serial number
of the device for each request.

```kotlin
class AdbActivityTest {
    @get:Rule
    val rule = ActivityScenarioRule(MainActivity::class.java)

    @get:Rule
    val adbRule = AdbRule(mode = Mode.ASSERT)

    @Test
    fun testVmState() {
        runBlocking {
            val result = adbRule.adb.execute(ShellCommandRequest("echo \"hello world\""))
            assert(result.exitCode == 0)
            assert(result.output.startsWith("hello world"))
        }
    }
}
```

## EmulatorGrpcRule

The rule for gRPC provides access to the supported gRPC requests and is only valid for an emulator. For a list of supported requests check
the official source [emulator_controller.proto][1]

No support for gRPC TLS or auth is currently implemented

```kotlin
class GrpcActivityTest {
    @get:Rule
    val rule = ActivityScenarioRule(MainActivity::class.java)

    @get:Rule
    val emulator = EmulatorGrpcRule(mode = Mode.ASSERT)

    @Test
    fun testVmState() {
        runBlocking {
            val vmState = emulator.grpc.getVmState(Empty.getDefaultInstance())
            assert(vmState.state == VmRunState.RunState.RUNNING)
        }
    }
}
```

## EmulatorConsoleRule

Emulator console port requires an auth token by default that is stored on the host OS of the emulator in
the `$HOME/.emulator_console_auth_token` file. If you want to get rid of the auth, leave an empty file at the same location. This will
prevent the emulator from requiring an auth token during the request.

```kotlin
class ConsoleActivityTest {
    @get:Rule
    val rule = ActivityScenarioRule(MainActivity::class.java)

    @get:Rule
    val console = EmulatorConsoleRule(mode = Mode.ASSERT)

    @Test
    fun testVmState() {
        runBlocking {
            val result = console.execute("avd status")
            Allure.description("VM state is $result")
            assert(result.contains("running"))
        }
    }
}
```

## Notes for developers of test runners

For real devices only adb access can be exposed. This is achieved via reverse port forwarding on the test runner's side. This works
regardless of the transport mode (direct USB connection or TCP via adb connect). The connection to adb can then be achieved via direct
communication to localhost on the real device. The port number that is by default 5037, can be different though. Only test runner has the
actual method of establishing the adb port (since test runner communicates with this adb server), hence test runner has to provide this port
number via Instrumentation arguments to the test.

For emulators there are two cases:
If the emulator is a local emulator, then access to the host's loopback interface (which has all the necessary services) can be achieved via
special 10.0.2.2 IP. For adb test runner should provide the port number for the same reasons as for real phones. For telnet communication
only test runner can calculate the port number (emulator-5554 -> 5554). Accessing this port requires auth token which has to be supplied by
test runner since there is no way to access host's file system for the $HOME/.emulator_console_auth_token. Same problem with the gRPC -
everything has to be supplied by the test runner.

If the emulator is a remote emulator connected via adb connect then it is not currently possible to establish the telnet port as well as
gRPC port. Although technically possible, in practice the adb port for such a case would most likely be port-forwarded already and there is
no way to guarantee that the adb connect port is actually equal to the real adb port. This would be possible to solve provided the emulator
was able to receive emulator-5554 variable somehow, but any Android properties would break on the load of a snapshot for example.

The contract for the test runner defines passing host and port of a provided adb/console/grpc connection. You can get it as a shared
dependency:

```
dependencies {
  implementation 'com.malinskiy.adam:android-testrunner-contract:X.X.X'
}
```

Here is roughly how it looks (please use the maven dependency and don't copy paste this):

```java
public class TestRunnerContract {
    public static String grpcPortArgumentName = "com.malinskiy.adam.android.GRPC_PORT";
    public static String grpcHostArgumentName = "com.malinskiy.adam.android.GRPC_HOST";
    public static String adbPortArgumentName = "com.malinskiy.adam.android.ADB_PORT";
    public static String adbHostArgumentName = "com.malinskiy.adam.android.ADB_HOST";
    public static String consolePortArgumentName = "com.malinskiy.adam.android.CONSOLE_PORT";
    public static String consoleHostArgumentName = "com.malinskiy.adam.android.CONSOLE_HOST";
    public static String emulatorAuthTokenArgumentName = "com.malinskiy.adam.android.AUTH_TOKEN";
    public static String deviceSerialArgumentName = "com.malinskiy.adam.android.ADB_SERIAL";
}
```

It is test runner's responsibility to set these up when executing tests, e.g.:

```bash
$ am instrument -w -r --no-window-animation -e class com.example.AdbActivityTest#testUnsafeAccess -e debug false -e com.malinskiy.adam.android.ADB_PORT 5037 -e com.malinskiy.adam.android.ADB_HOST 10.0.2.2 -e com.malinskiy.adam.android.ADB_SERIAL emulator-5554 -e com.malinskiy.adam.android.GRPC_PORT 8554 -e com.malinskiy.adam.android.GRPC_HOST 10.0.2.2 com.example.test/androidx.test.runner.AndroidJUnitRunner
```

[1]: https://android.googlesource.com/platform/prebuilts/android-emulator/+/master/linux-x86_64/lib/emulator_controller.proto "emulator_controller.proto"
