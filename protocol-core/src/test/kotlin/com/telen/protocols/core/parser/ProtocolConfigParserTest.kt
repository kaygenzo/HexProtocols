package com.telen.protocols.core.parser

import com.telen.protocols.core.model.RouteConfig
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.junit.jupiter.api.Test

// Local stand-ins for what will become the real transport-socket/transport-ble RouteConfig
// implementations once those modules exist (Phase 3/4) — protocol-core never depends on them.
@Serializable
@SerialName("tcp")
data class TestSocketRoute(
    val port: Int,
    val address: String? = null,
    val isBroadcast: Boolean = false
) : RouteConfig

@Serializable
@SerialName("udp")
data class TestUdpRoute(val port: Int, val isBroadcast: Boolean = false) : RouteConfig

@Serializable
@SerialName("ble")
data class TestBleRoute(val service: String, val characteristic: String) : RouteConfig

private val testModule =
    SerializersModule {
        polymorphic(RouteConfig::class) {
            subclass(TestSocketRoute::class, TestSocketRoute.serializer())
            subclass(TestUdpRoute::class, TestUdpRoute.serializer())
            subclass(TestBleRoute::class, TestBleRoute.serializer())
        }
    }

private val testJson =
    Json {
        ignoreUnknownKeys = true
        classDiscriminator = "type"
        serializersModule = testModule
    }

class ProtocolConfigParserTest {
    private val parser = ProtocolConfigParser(testJson)

    @Test
    fun `parses a synthetic device protocol end to end`() {
        val source =
            ProtocolSource {
                """
                {
                  "deviceNames": ["SYNTH"],
                  "commands": [
                    {
                      "identifier": "PING",
                      "request": {
                        "route": { "type": "tcp", "port": 1234 },
                        "length": 2,
                        "payloads": [
                          { "name": "OPCODE", "start": 0, "end": 0, "type": "HEX", "value": "0x01" },
                          { "name": "SEQ", "start": 1, "end": 1, "type": "INTEGER", "min": 0, "max": 255 }
                        ]
                      },
                      "response": {
                        "route": { "type": "tcp", "port": 1234 },
                        "frames": [
                          { "payloads": [ { "name": "STATUS", "start": 0, "end": 0, "type": "INTEGER" } ] }
                        ]
                      }
                    }
                  ]
                }
                """.trimIndent().byteInputStream()
            }

        val protocol = parser.parse(source)

        assertEquals(listOf("SYNTH"), protocol.deviceNames)
        val command = assertNotNull(protocol.command("PING"))
        val route = assertNotNull(command.request?.route as? TestSocketRoute)
        assertEquals(1234, route.port)
        assertEquals(2, command.request?.payloads?.size)
        assertNull(protocol.command("MISSING"))
    }

    @Test
    fun `parses a UDP route with the broadcast flag`() {
        val source =
            ProtocolSource {
                """
                {
                  "deviceNames": ["SYNTH-UDP"],
                  "commands": [
                    {
                      "identifier": "DISCOVER",
                      "request": {
                        "route": { "type": "udp", "port": 48899, "isBroadcast": true },
                        "layout": "TEXT",
                        "payloads": [ { "name": "MESSAGE", "type": "ASCII", "value": "PING" } ]
                      },
                      "response": { "route": { "type": "udp", "port": 48899 } }
                    }
                  ]
                }
                """.trimIndent().byteInputStream()
            }

        val protocol = parser.parse(source)

        val command = assertNotNull(protocol.command("DISCOVER"))
        val route = assertNotNull(command.request?.route as? TestUdpRoute)
        assertEquals(48899, route.port)
        assertEquals(true, route.isBroadcast)
    }

    @Test
    fun `parses a BLE route with a fixed-offset payload schema`() {
        val source =
            ProtocolSource {
                """
                {
                  "deviceNames": ["SYNTH-BLE"],
                  "commands": [
                    {
                      "identifier": "CHANGE_COLOR",
                      "request": {
                        "route": {
                          "type": "ble",
                          "service": "0000aaaa-0000-1000-8000-00805f9b34fb",
                          "characteristic": "0000bbbb-0000-1000-8000-00805f9b34fb"
                        },
                        "length": 4,
                        "payloads": [
                          { "name": "RED", "start": 0, "end": 0, "type": "INTEGER" },
                          { "name": "GREEN", "start": 1, "end": 1, "type": "INTEGER" },
                          { "name": "BLUE", "start": 2, "end": 2, "type": "INTEGER" },
                          { "name": "SUFFIX", "start": 3, "end": 3, "type": "HEX", "value": "0x00" }
                        ]
                      }
                    }
                  ]
                }
                """.trimIndent().byteInputStream()
            }

        val protocol = parser.parse(source)

        val command = assertNotNull(protocol.command("CHANGE_COLOR"))
        val route = assertNotNull(command.request?.route as? TestBleRoute)
        assertEquals("0000aaaa-0000-1000-8000-00805f9b34fb", route.service)
        assertEquals(4, command.request?.length)
        assertEquals(4, command.request?.payloads?.size)
    }
}
