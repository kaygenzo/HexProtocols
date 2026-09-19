package com.telen.protocols.core.codec

import com.telen.protocols.core.error.Outcome
import com.telen.protocols.core.model.FrameLayout
import com.telen.protocols.core.model.Payload
import com.telen.protocols.core.model.PayloadType
import com.telen.protocols.core.model.RequestSchema
import com.telen.protocols.core.model.RouteConfig
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

private object NoRoute : RouteConfig

class FrameCodecTest {
    @Test
    fun `encodes a fixed-length BINARY frame from defaults and overrides`() {
        val request =
            RequestSchema(
                route = NoRoute,
                length = 4,
                payloads =
                listOf(
                    Payload("SUBROUTINE", 0, 0, PayloadType.HEX, value = "0x71"),
                    Payload("COMMAND", 1, 1, PayloadType.HEX, value = "0x23"),
                    Payload("OPTION", 2, 2, PayloadType.HEX, value = "0x0F"),
                    Payload("CHECKSUM", 3, 3, PayloadType.INTEGER, min = "0", max = "255")
                )
            )
        val bytes = (FrameCodec.encode(request, mapOf("CHECKSUM" to 0x9D)) as Outcome.Success).value
        assertEquals("71230f9d", bytes.joinToString("") { "%02x".format(it) })
    }

    @Test
    fun `encodes a TEXT layout frame by sequential concatenation`() {
        val request =
            RequestSchema(
                route = NoRoute,
                layout = FrameLayout.TEXT,
                payloads =
                listOf(
                    Payload("MESSAGE", type = PayloadType.ASCII, value = "AT+WSSSID="),
                    Payload("SSID", type = PayloadType.ASCII),
                    Payload("END", type = PayloadType.ASCII, value = "\r")
                )
            )
        val bytes = (FrameCodec.encode(request, mapOf("SSID" to "MyWifi")) as Outcome.Success).value
        assertEquals("AT+WSSSID=MyWifi\r", String(bytes, Charsets.US_ASCII))
    }
}
