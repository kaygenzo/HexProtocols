package com.telen.protocols.core.codec

import com.telen.protocols.core.error.Outcome
import com.telen.protocols.core.model.FrameSchema
import com.telen.protocols.core.model.Payload
import com.telen.protocols.core.model.PayloadType
import com.telen.protocols.core.model.RequestSchema
import com.telen.protocols.core.model.ResponseSchema
import com.telen.protocols.core.model.RouteConfig
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

private object FakeApduRoute : RouteConfig

/**
 * Proves the schema/codec abstraction holds for a structurally different, future transport
 * (JavaCard/APDU: CLA/INS/P1/P2/Lc/Data + a SW1/SW2 trailer anchored to the end of the response)
 * using only the existing [Payload]/[RequestSchema]/[ResponseSchema]/[RouteConfig] types, plus the
 * additive negative-offset / "rest of frame" slicing already built into [FrameCodec.decode] and the
 * variable-length (`end = null`) support in [FrameCodec.encode] — no change to [RouteConfig],
 * [com.telen.protocols.core.transport.Transport], or any transport module was needed.
 */
class ApduReadinessTest {
    private val selectApplet =
        RequestSchema(
            route = FakeApduRoute,
            payloads =
            listOf(
                Payload("CLA", 0, 0, PayloadType.HEX, value = "0x00"),
                Payload("INS", 1, 1, PayloadType.HEX, value = "0xA4"),
                Payload("P1", 2, 2, PayloadType.HEX, value = "0x04"),
                Payload("P2", 3, 3, PayloadType.HEX, value = "0x00"),
                Payload("LC", 4, 4, PayloadType.INTEGER),
                Payload("DATA", 5, null, PayloadType.HEX_STRING)
            )
        )

    private val selectAppletResponse =
        ResponseSchema(
            route = FakeApduRoute,
            frames =
            listOf(
                FrameSchema(
                    payloads =
                    listOf(
                        Payload("DATA", 0, -3, PayloadType.HEX_STRING),
                        Payload("SW1", -2, -2, PayloadType.HEX),
                        Payload("SW2", -1, -1, PayloadType.HEX)
                    )
                )
            )
        )

    @Test
    fun `encodes a variable-length APDU command with a Lc-driven trailing DATA field`() {
        val aid = "A000000151000000"
        val bytes = (
            FrameCodec.encode(
                selectApplet,
                mapOf("LC" to aid.length / 2, "DATA" to aid)
            ) as Outcome.Success
            ).value
        assertEquals("00a4040008" + aid.lowercase(), bytes.joinToString("") { "%02x".format(it) })
    }

    @Test
    fun `decodes a status trailer anchored to the end of a variable-length APDU response`() {
        val raw = byteArrayOf(0x6F.toByte(), 0x10.toByte(), 0x90.toByte(), 0x00.toByte())
        val frame = (FrameCodec.decode(selectAppletResponse, raw) as Outcome.Success).value
        assertEquals("6f10", frame.values["DATA"])
        assertEquals("0x90", frame.values["SW1"])
        assertEquals("0x00", frame.values["SW2"])
    }
}
