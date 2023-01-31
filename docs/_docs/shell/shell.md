---
layout: default
title: Shell
nav_order: 1
has_children: true
has_toc: false
permalink: /docs/shell
---

1. TOC
{:toc}

This is a description of requests in `com.malinskiy.adam.request.shell.v1`

## Execute shell command

You can execute arbitrary commands (`ls`, `date`, etc) on the device using the `ShellCommandRequest`:

```kotlin
val response: ShellCommandResult = adb.execute(
    request = ShellCommandRequest("echo hello"),
    serial = "emulator-5554"
)
```

The response contains stdout mixed with stderr (since this protocol doesn't support separate streams).
On top of this, shell v1 doesn't support returning an exit code natively. To mitigate this whenever you execute any shell v1 command adam
appends `;echo $?` to the end of the command and parses it automatically.

```kotlin
data class ShellCommandResult(
    val stdout: ByteArray,
    val exitCode: Int
)
```

If the output is UTF-8 encoded then you can use lazy property `output` for conversion of bytes into a String, e.g. `result.output`.

This request expects that the command returns immediately, or you don't want to stream the output.

## Streaming shell request

You can execute arbitrary commands (`cat`, `tail -f`, etc) on the device using the `ChanneledShellCommandRequest`:

```kotlin
launch {
    val updates = adb.execute(
        request = ChanneledShellCommandRequest("logcat -v"),
        scope = this,
        serial = "emulator-5554"
    )

    for (lines in updates) {
        println(lines)
    }
}
```

## Execute shell command with pipe input

Executes the command and provides the channel as the input to the command. Does not return anything

```kotlin
val blockSizeChannel = Channel<Int>(capacity = 1)
//You have to implement the function below for applicable source of data that you have. 
//Testing code in adam has an example for a file
val channel: ReceiveChannel<ByteArray> = someFunctionThatProducesByteArrayInResponseToRequestsOverBlockSizeChannel(blockSizeChannel)
val success = client.execute(
    request = ExecInRequest(
        cmd = "cmd package install -S ${testFile.length()}",
        channel = testFile.readChannel(),
        sizeChannel = blockSizeChannel
    ),
    serial = "emulator-5554"
)
```
