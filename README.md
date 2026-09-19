[![GitHub license](https://img.shields.io/github/license/kaygenzo/HexProtocols.svg)](https://github.com/kaygenzo/HexProtocols/blob/develop/LICENSE)

# Hex Protocols

Hex Protocols lets you drive a hardware device — over BLE, TCP/UDP sockets, or any other transport
— by describing its wire protocol in a JSON file, instead of hand-writing byte-packing code for
every command. You author a schema (which bytes go where, what type they are, what a response
looks like); the library builds the outgoing frame, sends it, and decodes the response back into
named values.

The core schema, codec, and orchestration engine know nothing about BLE, sockets, or any other
specific technology — a `Transport` and a `RouteConfig` implementation is all a new technology
needs to plug in. See [Architecture](#architecture) below.

## Modules

| Module | What it is |
| --- | --- |
| `protocol-core` | The transport-agnostic schema, codec, `ProtocolEngine`, and JSON parser. Depend on this plus whichever transport(s) you need. |
| `transport-socket` | TCP/UDP `Transport` implementation (`TcpRoute`, `UdpRoute`). |
| `transport-ble` | BLE `Transport` implementation (`BleRoute`), built on a small `GattApi` seam so the GATT-calling code stays out of your test suite. |
| `sample-app` | A minimal Compose app driving a socket device and a BLE device through nothing but the public API — the best place to see real usage end to end. |

## Installation

This project isn't published to a public repository yet. Until it is, the most direct way to
consume it is a Gradle composite build (`includeBuild("path/to/HexProtocols")` in your
`settings.gradle.kts`), or to publish it to your local Maven cache and depend on that:

```bash
./gradlew publishToMavenLocal
```

```kotlin
// your project's settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        mavenLocal()
        // ... your other repositories
    }
}
```

```kotlin
// your module's build.gradle.kts
dependencies {
    implementation("com.telen.protocols:protocol-core:0.1.0")
    implementation("com.telen.protocols:transport-socket:0.1.0") // and/or
    implementation("com.telen.protocols:transport-ble:0.1.0")
}
```

## Usage

Parse a device's protocol file (an Android asset here, but `ProtocolSource` is just a functional
interface — anything that hands back an `InputStream` works):

```kotlin
val json = Json {
    classDiscriminator = "type"
    serializersModule = SocketRouteModule + BleRouteModule // compose whichever transports you use
}
val protocol = ProtocolConfigParser(json).parse(AssetProtocolSource(context, "my_device.json"))
```

Drive a socket device:

```kotlin
val engine = ProtocolEngine(SocketTransport())
val command = protocol.command("LIGHT_ON")!!
val route = (command.request!!.route as TcpRoute).copy(address = "192.168.1.50")
engine.execute(command, values = mapOf("CHECKSUM" to checksum), routeOverride = route)
    .collect { result -> /* Outcome.Success or Outcome.Failure */ }
```

`routeOverride` is how you supply information the schema can't know ahead of time — an IP address
found via a discovery command, for instance — without mutating anything shared: each call gets an
immutable, independently addressed route.

Drive a BLE device:

```kotlin
val transport = BleTransport(AndroidGattApi(context))
transport.connect(BleTarget(macAddress = "AA:BB:CC:DD:EE:FF"))
engine.execute(protocol.command("CHANGE_COLOR")!!, values = mapOf("RED" to 255, "GREEN" to 0, "BLUE" to 0))
```

See `sample-app` for a complete, runnable example of both.

## Protocol file format

A protocol file describes one device: its name(s), and the commands it understands.

```json
{
  "deviceNames": ["MyDevice"],
  "commands": [
    {
      "identifier": "COMMAND_NAME",
      "request": { "...": "see below" },
      "response": { "...": "see below, optional — omit for fire-and-forget commands" }
    }
  ]
}
```

`request`/`response` share the same shape:

```json
{
  "route": { "type": "tcp", "port": 5577 },
  "layout": "BINARY",
  "length": 4,
  "timeout": 0,
  "payloads": [ { "...": "see below" } ],
  "frames": [ { "commandId": 1, "commandIndex": 0, "payloads": [ "..." ] } ]
}
```

- **`route`** is polymorphic on a `"type"` discriminator, owned by whichever transport module you
  depend on: `{ "type": "tcp", "port": ... }` / `{ "type": "udp", "port": ..., "isBroadcast": ... }`
  from `transport-socket`, or `{ "type": "ble", "service": "...", "characteristic": "..." }` from
  `transport-ble`. A new transport contributes its own route shape without touching `protocol-core`
  or any existing transport.
- **`layout`** is `BINARY` (payloads packed at fixed byte offsets — the default) or `TEXT` (payloads
  concatenated in declared order with no offsets, for AT-command-style protocols).
- **`length`** pads a `BINARY` frame to at least this many bytes.
- **`timeout`** (milliseconds) bounds how long a response is awaited; omit or leave at `0` for no
  timeout.
- **`frames`** (response only) lists the possible shapes an incoming message can take, disambiguated
  by a discriminator byte at `commandIndex` equal to `commandId`. A single frame needs no
  discriminator.

A `payload` is one field within a frame:

```json
{
  "name": "RED",
  "start": 0,
  "end": 0,
  "type": "INTEGER",
  "direction": "LTR",
  "value": "0",
  "min": "0",
  "max": "255"
}
```

- **`start`/`end`** are inclusive byte offsets (only meaningful for `BINARY` layout). Either can be
  negative, counted from the end of the frame (Kotlin-slice style), and `end` can be omitted to mean
  "the rest of the frame" — useful for a variable-length field followed by a fixed-size trailer.
- **`type`** is one of `HEX`, `HEX_STRING`, `STRING`, `INTEGER`, `LONG`, `ASCII`.
- **`direction`** is `LTR` (default) or `RTL` to reverse byte order.
- **`value`** is a fixed/default value, used when the caller doesn't supply one for this payload's
  `name` in the `values` map passed to `ProtocolEngine.execute`.
- **`min`**/**`max`** bound `INTEGER`/`LONG` values, enforced on both encode and decode.

## Extending to a new transport

1. Depend on `protocol-core` only.
2. Implement `RouteConfig` (a plain marker interface — no shared base class to touch) for whatever
   addressing information your technology needs, and register it into a `SerializersModule` your
   app composes at JSON-parsing time.
3. Implement `Transport`: `connect`/`disconnect` for whatever session concept your technology has
   (or a no-op if it doesn't — see `SocketTransport`), `send` to write a frame, `observe` to expose
   incoming frames as a `Flow<ByteArray>`.

`protocol-core`'s schema, codec, and `ProtocolEngine` never change — see
`protocol-core`'s `ApduReadinessTest` for a worked proof that the same model already covers a
structurally different transport (APDU/ISO-7816 command/response framing) before a single line of
transport code exists.

## Testing

Every module's tests run against real I/O where that's possible (real loopback sockets in
`transport-socket`) or a faked seam where it isn't (BLE requires real hardware, so `transport-ble`
fakes its `GattApi` instead of Android's `BluetoothGatt`). None of the library modules ship or
depend on any specific device's protocol file — those belong to the app that uses the library, as
`sample-app`'s assets demonstrate.

```bash
./gradlew build
```

## Contributing

Pull requests are welcome. For major changes, please open an issue first to discuss what you would
like to change. Please make sure to update tests as appropriate.

## License

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
