[![GitHub license](https://img.shields.io/github/license/kaygenzo/HexProtocols.svg)](https://github.com/kaygenzo/HexProtocols/blob/develop/LICENSE)
[![GitHub version](https://badge.fury.io/gh/kaygenzo%2FHexProtocols.svg)](https://github.com/kaygenzo/HexProtocols)


# Hex Protocols

This project intends to provide an easy way to interact with BLE and Sockets devices by describing commands through a simple protocol file.

## Installation of BLE library

First, you will need at least the basic protocols library which is common to all the hardware provided libraries. Then, you will need to add the BLE library or/and the socket library to interact with the remote devices.


```bash
implementation "com.telen.library:common-protocols:x.y.z"
implementation "com.telen.library:ble-protocols:x.y.z"
implementation "com.telen.library:socket-protocols:x.y.z"
```

## Usage

First you need to define a protocol file. The protocol file is a json file with a specific structure. It has to be located into the assets of your project. The file name will be injected into an object DeviceConfiguration which will parse the json file and let you use the library. To create a device configuration object, simply use

```Java
ProtocolConfiguration.parse(mContext,"filename");
```

Then you need to get an instance of the data layer and link it to the desired hardware layer.

To get the instance of the BLE data layer

```java
import com.telen.sdk.ble.di.BleManager;
DataLayerInterface<BleHardwareConnectionLayer> dataLayer = BleManager.getInstance(context).getDataLayer();
```

To get the instance of the socket data layer

```java
import com.telen.sdk.socket.di.SocketManager;
DataLayerInterface<SocketHardwareConnectionLayer> dataLayer = SocketManager.getInstance(context).getDataLayer();
```

Finally you will need an instance of a Device object to interact with all layers.

```java
import com.telen.sdk.common.models.Device;
Device device = new Device(deviceName, macAddress);
```

The socket library define an inherited object

```java
import com.telen.sdk.socket.devices.SocketDevice;
final SocketDevice mDevice = new SocketDevice.Builder()
            .withName("LED")
            .withAddress("localhost")
            .withPort(65000)
            .withType(RequestType.tcp)
            .build();
```
Now you are ready to begin!

Launching a command:

```java
Map<String, Object> data = new HashMap<>();
data.put("RED",0);
data.put("GREEN",0);
data.put("BLUE",0);
data.put("LUMINOSITY_1",255);
data.put("LUMINOSITY_2",255);
return dataLayer.sendCommand(device, deviceConfiguration.getCommand("CHANGE_COLOR"),data)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread());
```

Connect to a remote device

```java
return dataLayer.connect(device, createBond)
               .subscribeOn(Schedulers.io())
               .observeOn(AndroidSchedulers.mainThread());
```

### BLE file protocol

```json
{
  "deviceNames": ["deviceName[MANDATORY]"],
  "commands": [
    {
      "identifier": "your_identifier[MANDATORY]",
      "request": {
        "service": "your_service_uuid[MANDATORY]",
        "characteristic": "your_characteristic_uuid[MANDATORY]",
        "length": "wanted_bytes_length",
        "payloads": [
          <!-- Payload structure, see below-->
        ]
      },
      "response": {
        "service": "your_service_uuid[MANDATORY]",
        "characteristic": "your_characteristic_uuid[MANDATORY]",
        "type": "indication|notification",
        "length": "expected_bytes_length",
        "frames": [
          {
            "commandId": "command_identifier_value[MANDATORY]",
            "commandIndex": "command_identifier_index[MANDATORY]",
            "payloads": [
              <!-- Payload structure, see below-->
            ]
          }
        ]
      }
    }
  ]
}
```

### Socket file protocol

```json
{
  "deviceNames": ["deviceName[MANDATORY]"],
  "commands": [
    {
      "identifier": "your_identifier[MANDATORY]",
      "request": {
        "address": "address",
        "port": "port",
        "type": "udp|tcp[MANDATORY]",
        "length": "wanted_bytes_length",
        "payloads": [
          <!-- Payload structure, see below-->
        ]
      },
      "response": {
        "type": "udp|tcp[MANDATORY]",
        "isBroadcast": "true|false",
        "length": "expected_bytes_length",
        "frames": [
          {
            "commandId": "command_identifier_value[MANDATORY]",
            "commandIndex": "command_identifier_index[MANDATORY]",
            "payloads": [
              <!-- Payload structure, see below-->
            ]
          }
        ]
      }
    }
  ]
}
```

The blocks request and response are not mandatory. However if you define them, please do be careful around mandatory fields inside the defined objects.

The Payload object is defined as follow:

```json
{
  "name": "command_identifier",
  "direction": "LTR|RTL",
  "start": "byte_start",
  "end": "byte_end",
  "type": "byte_type",
  "value": "byte_default_value",
  "min": "byte_min_value",
  "max": "byte_max_value"
}
```

Type of payload can be either:

| Type       | Description |
| ---------- | ----------- |
| INTEGER    | Integer value |
| LONG       | Long value |
| HEX        | String value representing hexadecimal value. For instance "0xFFFF" |
| HEX_STRING | Hexa string which will be cut by packet of 2 characters and transformed into java byte. Example "0FFF" |
| STRING     | String which will be converted to hexa value, and converted into java byte. Ex "FRANCE" -> "4652414e4345" -> java bytes
| ASCII      | Raw ascii which will be directly converted to java bytes. Ex "ALT+Z" -> java bytes

### Examples

If you want to listen to the answers on a ble characteristic, an example of
json protocol file could be:

```json
{
  "deviceNames": ["MyDevice"],
  "commands": [
    {
      "identifier": "AWSOME_COMMAND",
      "response": {
        "service": "0000XXXX-0000-1000-8000-00805f9b34fb",
        "characteristic": "0000YYYY-0000-1000-8000-00805f9b34fb",
        "type": "indication",
        "length": 20,
        "frames": [
          {
            "commandId": 160,
            "commandIndex": 0,
            "payloads": [
              {
                "name": "COMMAND_ID",
                "start": 0,
                "end": 0,
                "type": "HEX",
                "value": "0xa0"
              },
              {
                "name": "PASSWORD",
                "start": 1,
                "end": 4,
                "type": "HEX_STRING"
              }
            ]
          },
          {
            "commandId": 161,
            "commandIndex": 0,
            "payloads": [
              {
                "name": "COMMAND_ID",
                "start": 0,
                "end": 0,
                "type": "HEX",
                "value": "0xa1"
              },
              {
                "name": "AWSOME_NUMBER",
                "start": 1,
                "end": 4,
                "type": "HEX_STRING"
              }
            ]
          },
          {
            "commandId": 162,
            "commandIndex": 0,
            "payloads": [
              {
                "name": "COMMAND_ID",
                "start": 0,
                "end": 0,
                "type": "HEX",
                "value": "0x83"
              },
              {
                "name": "AGE",
                "start": 1,
                "end": 1,
                "type": "INTEGER",
                "min": 1,
                "max": 255
              }
            ]
          }
        ]
      }
    }
  ]
}
```

In this case you listen to multiple different responses on the characteristic 0000YYYY-0000-1000-8000-00805f9b34fb, each responses will be represented by an id at the byte index 0, and the values of this byte can be [160-162].

If you want to launch a command and don't care about the response:

```json
{
  "identifier": "SEND_TIME",
  "request": {
    "service": "0000XXXX-0000-1000-8000-00805f9b34fb",
    "characteristic": "0000YYYY-0000-1000-8000-00805f9b34fb",
    "length": 5,
    "payloads": [
      {
        "name": "COMMAND_ID",
        "start": 0,
        "end": 0,
        "type": "HEX",
        "value": "0x02"
      },
      {
        "name": "UTC",
        "direction": "RTL",
        "start": 1,
        "end": 4,
        "type": "LONG"
      }
    ]
  }
}
```

If you want to launch a socket command over Wifi

```json
{
  "deviceNames": ["LED"],
  "commands": [
    {
      "identifier": "REBOOT",
      "request": {
        "type": "udp",
        "port": 48899,
        "payloads": [
          {
            "name": "MESSAGE",
            "type": "ASCII",
            "value": "AT+Z\r"
          }
        ]
      },
      "response": {
        "type": "udp"
      }
    },
    {
     "identifier": "GET_REMOTE_ADDRESS",
     "request": {
       "type": "udp",
       "port": 48899,
       "isBroadcast": true,
       "payloads": [
         {
           "name": "MESSAGE",
           "type": "ASCII",
           "value": "HF-A11ASSISTHREAD"
         }
       ]
     },
     "response": {
       "type": "udp"
     }
   }
  ]
}
```

Find more examples within the assets of the demo application.

## Contributing
Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

Please make sure to update tests as appropriate.

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