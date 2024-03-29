---
layout: default
title:  "Shell v2"
parent: Shell
nav_order: 2
---

This is a description of requests in `com.malinskiy.adam.request.shell.v2`

## Execute shell command
Requires Feature.SHELL_V2 
{: .label .label-yellow }

You can execute arbitrary commands (`ls`, `date`, etc) on the device using the `ShellCommandRequest`:

```kotlin
val response: ShellCommandResult = adb.execute(
    request = ShellCommandRequest("echo hello"),
    serial = "emulator-5554"
)
```

The response contains separate stdout and stderr as well as an exit code.

```kotlin
data class ShellCommandResult(
    val stdout: ByteArray,
    val stderr: ByteArray,
    val exitCode: Int
)
```

If the output is UTF-8 encoded then you can use lazy properties `output` and `errorOutput` for conversion of bytes into a String,
e.g. `result.output`.

This request expects that the command returns immediately, or you don't want to stream the output.

## Streaming shell request

Requires Feature.SHELL_V2
{: .label .label-yellow }

You can execute arbitrary commands (`cat`, `tail -f`, etc) on the device using the `ChanneledShellCommandRequest`. Shell v2 brings in
support for stdin implemented as a separate channel.

```kotlin
launch {
    val stdio = Channel<ShellCommandInputChunk>()
    val receiveChannel = adb.execute(ChanneledShellCommandRequest("cat", stdio), this, "emulator-5554")
    //Sending commands requires additional pool, otherwise we might deadlock
    val stdioJob = launch(Dispatchers.IO) {
        stdio.send(
            ShellCommandInputChunk(
                stdin = "cafebabe".toByteArray(Charsets.UTF_8)
            )
        )

        stdio.send(
            ShellCommandInputChunk(
                close = true
            )
        )
    }

    val stdoutBuilder = StringBuilder()
    val stderrBuilder = StringBuilder()
    var exitCode = 1
    for (i in receiveChannel) {
        i.stdout?.let { stdoutBuilder.append(String(it, Charsets.UTF_8)) }
        i.stderr?.let { stderrBuilder.append(String(it, Charsets.UTF_8)) }
        i.exitCode?.let { exitCode = it }
    }
    stdioJob.join()

    println(stdoutBuilder.toString())
}
```
