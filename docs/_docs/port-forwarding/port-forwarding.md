---
layout: default
title:  "Port forwarding"
nav_order: 9
---

## Port forwarding and reverse port forwarding

{: .no_toc }

1. TOC {:toc}

# Port-forwarding

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
    serial = "emulator-5554",
    mode = DEFAULT)
) 
```

`DEFAULT` mode does not rebind the port. If you need to rebind use the `NO_REBIND` value.

## List port forwarding rules

To retrieve a list of port forwarding rules use the following:

```kotlin
val rules: List<PortForwardingRule> = adb.execute(ListPortForwardsRequest("emulator-5554"))
```

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

# Reverse port-forwarding

## Create reverse port forwarding rule

This request forwards some remote device port to a local host port.

Remote(host) port can be defined as:

- `LocalTcpPortSpec(val port: Int)`. This will map a local TCP port.
- `LocalUnixSocketPortSpec(val path: String)`. This will create a local named unix path.

Local(device) port can be defined as:

- `RemoteTcpPortSpec(val port: Int)`. This will map a remote TCP port.
- `RemoteAbstractPortSpec(val unixDomainSocketName: String)`
- `RemoteReservedPortSpec(val unixDomainSocketName: String)`
- `RemoteFilesystemPortSpec(val unixDomainSocketName: String)`
- `RemoteDevPortSpec(val charDeviceName: String)`
- `JDWPPortSpec(val processId: Int)`

```kotlin
adb.execute(request = ReversePortForwardRequest(
    local = RemoteTcpPortSpec(12042), 
    remote = LocalTcpPortSpec(12042), 
    serial = "emulator-5554",
    mode = DEFAULT)
) 
```

`DEFAULT` mode does not rebind the port. If you need to rebind use the `NO_REBIND` value.

## List reverse port forwarding rules

To retrieve a list of reverse port forwarding rules use the following:

```kotlin
val rules: List<ReversePortForwardingRule> = adb.execute(ListReversePortForwardsRequest(), "emulator-5554")
```

## Remove a reverse port forwarding rule

To remove a forwarding rule you don't need to specify a remote port spec.

```kotlin
adb.execute(request = RemoveReversePortForwardRequest(
    local = RemoteTcpPortSpec(12042), 
    serial = "emulator-5554")
) 
```

## Remove all reverse port forwards

To clean all the rules:

```kotlin
adb.execute(request = RemoveAllReversePortForwardsRequest(
    serial = "emulator-5554"
)) 
```
