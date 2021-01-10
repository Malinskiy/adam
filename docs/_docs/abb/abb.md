---
layout: default
title: "Android Binder Bridge"
nav_order: 10
has_toc: false
permalink: /docs/abb
---

Android Binder Bridge gives the ability to communicate directly with the services on the devices. An example of service is `package` which
is handling the package management. To see the full list available services use `-l` on the abb request.

## Execute with stdout, stderr and exit code

Requires Feature.ABB {: .label .label-yellow }

Executing something on a service is equivalent to executing an arbitrary `cmd` sub-command (`cmd package list`,
`cmd statusbar expand-notifications`, etc) on the device. Here is an example of listing currently available services:

```kotlin
val result = adb.execute(
    request = AbbRequest(listOf("-l")),
    serial = "emulator-5554"
)

println(result.stdout)
```

This will give you the result of the execution that includes stdout, stderr and exit code.

## Executing with stdout only

Requires Feature.ABB_EXEC {: .label .label-yellow }

Some devices will not support the abb requests, instead they will support abb_exec that only returns stdout.

```kotlin
val stdout = adb.execute(
    request = AbbExecRequest(listOf("-l")),
    serial = "emulator-5554"
)

println(stdout)
```