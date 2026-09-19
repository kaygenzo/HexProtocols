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
    fun `parses the real led_ribbon fixture`() {
        val source =
            ProtocolSource {
                requireNotNull(javaClass.classLoader!!.getResourceAsStream("led_ribbon.json"))
            }
        val protocol = parser.parse(source)

        assertEquals(listOf("LED"), protocol.deviceNames)

        val lightOn = assertNotNull(protocol.command("LIGHT_ON"))
        assertEquals(TestSocketRoute(port = 5577), lightOn.request?.route)

        val getRemoteAddress = assertNotNull(protocol.command("GET_REMOTE_ADDRESS"))
        val route = assertNotNull(getRemoteAddress.request?.route as? TestUdpRoute)
        assertEquals(48899, route.port)
        assertEquals(true, route.isBroadcast)
    }

    @Test
    fun `parses the real minger fixture`() {
        val source =
            ProtocolSource {
                requireNotNull(javaClass.classLoader!!.getResourceAsStream("minger.json"))
            }
        val protocol = parser.parse(source)

        assertEquals(listOf("minger-P50"), protocol.deviceNames)
        val changeColor = assertNotNull(protocol.command("CHANGE_COLOR"))
        val route = assertNotNull(changeColor.request?.route as? TestBleRoute)
        assertEquals("00007777-0000-1000-8000-00805f9b34fb", route.service)
        assertEquals(20, changeColor.request?.length)
        assertEquals(8, changeColor.request?.payloads?.size)
    }
}
