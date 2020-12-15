---
layout: default
title:  "Port forwarding"
parent: Sync requests
nav_order: 6
---

## Create port forwarding rule

This request forwards some local port to a remote device port.

Local port can be defined as:
- `LocalTcpPortSpec(val port: Int)`. This will map a local TCP port.
- `LocalUnixSocketPortSpec(val path: String)`. This will create a local named unix path.

Remote port can be defined as:
- `RemoteTcpPortSpec(val port: Int)`. This will map a remote TCP port.
- `RemoteAbstractPortSpec(val unixDomainSocketName: String)`
- `RemoteReservedPortSpec(val unixDomainSocketName: String)`
- `RemoteFilesystemPortSpec(val unixDomainSocketName: String)`
- `RemoteDevPortSpec(val charDeviceName: String)`
- `JDWPPortSpec(val processId: Int)`

```kotlin
adb.execute(request = PortForwardRequest(
    local = LocalTcpPortSpec(12042), 
    remote = RemoteTcpPortSpec(12042), 
    serial = adbRule.deviceSerial,
    mode = DEFAULT)
) 
```

`DEFAULT` mode does not rebind the port. If you need to rebind use the `NO_REBIND` value.

## Remove a port forwarding rule
To remove a forwarding you don't need to specify the remote port spec.

```kotlin
adb.execute(request = RemovePortForwardRequest(
    local = LocalTcpPortSpec(12042), 
    serial = "emulator-5554")
) 
```

## Remove all port forwards
To clean all the rules:

```kotlin
adb.execute(request = RemoveAllPortForwardsRequest(
    serial = "emulator-5554"
)) 
```
