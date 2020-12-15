---
layout: default
title:  "Device properties (getprop)"
parent: Sync requests
nav_order: 3
---

Retrieving device properties (equivalent to executing `getprop` on the device) can be done using the requests below.

## Get all device properties
This requests retrieves all properties and create a Map of String -> String to allow working with properties like this
 `properties["sys.boot_completed"]`.

```kotlin
val properties: Map<String, String> = adb.execute(
    request = GetPropRequest(),
    serial = "emulator-5554" 
)
```

## Get single device property
If only a single property is needed then you can use the shorter version:

```kotlin
val value: String = adb.execute(
    request = GetSinglePropRequest(name = "sys.boot_completed"),
    serial = "emulator-5554" 
)
```