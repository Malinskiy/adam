---
layout: default title:  "mDNS"
nav_order: 7
---

mDNS is used for automatic discovery and connection of remote devices (for example with Android 11 ADB over WiFi)

## Check if mdns discovery is available

```kotlin
val status: MdnsStatus = adb.execute(MdnsCheckRequest())
```

## List all discovered services

```kotlin
val services: List<MdnsService> = adb.execute(ListMdnsServicesRequest())
```

```kotlin
data class MdnsService(
    val name: String,
    val serviceType: String,
    val url: String
)
```
