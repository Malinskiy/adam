---
layout: default
title: "Emulator"
nav_order: 4
has_toc: false
permalink: /docs/emu
---

There are three ways to control an emulator:

1. Regular adb commands
2. Using a console port
3. Using a gRPC port

At boot time each emulator allocates 2 ports on the host's loopback interface: a _console_ port and a _gRPC_ bridge port. The console port
can be established using the serial number of emulator, for example `emulator-5554`'s console port is 5554, `emulator-5556` - 5556. If you
want to make sure emulator uses a particular console port you can start the emulator with a parameter:

```bash
$ emulator @Nexus_5X_API_23 -port <port>
```

gRPC bridge port is usually calculated as `console port + 3000`, so for `emulator-5554` default gRPC port will be 8554. If you want to make
sure emulator uses a particular grpc bridge port you can start the emulator with a parameter:

```bash
$ emulator @Nexus_5X_API_23 -grpc <port>
```

## Run emulator console command

This request will execute an emulator console command:

```kotlin
val devices: List<Device> = adb.execute(
    request =
    EmulatorCommandRequest(
        "help",
        InetSocketAddress("localhost", 5556)
    )
)
```

This request is completely different to other requests: it connects directly to emulator instead of adb server. For simplicity, it can be
used in the same way as adb server requests and shares the socket creation logic with other requests.

## Run gRPC bridge commands

Adam bundles a gRPC client for emulator gRPC bridge. The spec is generated from [emulator_controller.proto][1]

Please refer to the gRPC docs on using the client. A very simple example looks something like this:

```kotlin
val channel = ManagedChannelBuilder.forAddress(grpcHost, grpcPort).apply {
    usePlaintext()
    executor(Dispatchers.IO.asExecutor())
}.build()
val client = EmulatorControllerGrpcKt.EmulatorControllerCoroutineStub(channel)
val state = client.getVmState(Empty.getDefaultInstance())
```

Please refer to the [emulator_controller.proto][1] for the supported functionality.

[1]: https://android.googlesource.com/platform/prebuilts/android-emulator/+/master/linux-x86_64/lib/emulator_controller.proto "emulator_controller.proto"
