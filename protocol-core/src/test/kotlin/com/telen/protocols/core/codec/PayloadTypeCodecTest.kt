package com.telen.protocols.core.codec

import com.telen.protocols.core.error.Outcome
import com.telen.protocols.core.model.ByteOrder
import com.telen.protocols.core.model.Payload
import com.telen.protocols.core.model.PayloadType
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class PayloadTypeCodecTest {
    @Test
    fun `integer payload round trips and enforces bounds`() {
        val payload = Payload("LEVEL", 0, 0, PayloadType.INTEGER, min = "0", max = "255")
        val encoded = IntegerCodec.encode(payload, 200) as Outcome.Success
        assertEquals(1, encoded.value.size)
        val decoded = IntegerCodec.decode(payload, encoded.value) as Outcome.Success
        assertEquals(200, decoded.value)

        assertTrue(IntegerCodec.encode(payload, 300) is Outcome.Failure)
        assertTrue(IntegerCodec.encode(payload, -1) is Outcome.Failure)
    }

    @Test
    fun `direction reverses byte order symmetrically`() {
        val ltr = Payload("VALUE", 0, 1, PayloadType.INTEGER)
        val rtl = ltr.copy(direction = ByteOrder.RTL)

        val ltrBytes = (IntegerCodec.encode(ltr, 0x0102) as Outcome.Success).value
        val rtlBytes = (IntegerCodec.encode(rtl, 0x0102) as Outcome.Success).value
        assertEquals(ltrBytes.toList(), rtlBytes.reversed())
        assertEquals(0x0102, (IntegerCodec.decode(rtl, rtlBytes) as Outcome.Success).value)
    }

    @Test
    fun `hex string round trips raw hex bytes`() {
        val payload = Payload("PREFIX", 0, 3, PayloadType.HEX_STRING)
        val encoded = (HexStringCodec.encode(payload, "01fe0000") as Outcome.Success).value
        assertEquals("01fe0000", (HexStringCodec.decode(payload, encoded) as Outcome.Success).value)
    }

    @Test
    fun `ascii encodes text as raw bytes without hex conversion`() {
        val payload = Payload("MESSAGE", type = PayloadType.ASCII)
        val encoded = (AsciiCodec.encode(payload, "AT+Z\r") as Outcome.Success).value
        assertEquals("AT+Z\r", String(encoded, Charsets.US_ASCII))
    }
}
