---
layout: default title:  "Shell v1"
parent: Shell nav_order: 1
---

## Execute shell command

You can execute arbitrary commands (`ls`, `date`, etc) on the device using the `ShellCommandRequest`:

```kotlin
val response: ShellCommandResult = adb.execute(
    request = ShellCommandRequest("echo hello"),
    serial = "emulator-5554"
)
```

The response contains stdout mixed with stderr (since this protocol doesn't support separate streams) and an exit code.

```kotlin
data class ShellCommandResult(
    val output: String,
    val exitCode: Int
)
```

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
val testFile = File(javaClass.getResource("/app-debug.apk").toURI())
val success = client.execute(
    request = ExecInRequest(
        cmd = "cmd package install -S ${testFile.length()}",
        channel = testFile.readChannel()
    ),
    serial = "emulator-5554"
)
```