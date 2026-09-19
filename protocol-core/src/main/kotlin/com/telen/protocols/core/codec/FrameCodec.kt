package com.telen.protocols.core.codec

import com.telen.protocols.core.error.Outcome
import com.telen.protocols.core.error.ProtocolError
import com.telen.protocols.core.model.FrameLayout
import com.telen.protocols.core.model.FrameSchema
import com.telen.protocols.core.model.Payload
import com.telen.protocols.core.model.RequestSchema
import com.telen.protocols.core.model.ResponseSchema

data class DecodedFrame(val values: Map<String, Any>, val raw: ByteArray)

object FrameCodec {
    fun encode(
        request: RequestSchema,
        values: Map<String, Any?>
    ): Outcome<ByteArray, ProtocolError> = when (request.layout) {
        FrameLayout.BINARY -> encodeBinary(request.payloads, values, request.length)
        FrameLayout.TEXT -> encodeText(request.payloads, values)
    }

    private fun encodeBinary(
        payloads: List<Payload>,
        values: Map<String, Any?>,
        declaredLength: Int
    ): Outcome<ByteArray, ProtocolError> {
        val placements = mutableListOf<Pair<Int, ByteArray>>()
        for (payload in payloads) {
            val start =
                payload.start ?: return Outcome.Failure(
                    ProtocolError.Encoding(
                        payload.name,
                        "BINARY layout requires start"
                    )
                )
            val resolved = values[payload.name] ?: payload.value
            when (val encoded = PayloadCodecs.forType(payload.type).encode(payload, resolved)) {
                is Outcome.Failure -> return encoded
                is Outcome.Success -> {
                    val bytes = encoded.value
                    payload.end?.let { end ->
                        val expected = end - start + 1
                        if (bytes.size != expected) {
                            return Outcome.Failure(
                                ProtocolError.Encoding(
                                    payload.name,
                                    "encoded to ${bytes.size} bytes, expected $expected"
                                )
                            )
                        }
                    }
                    placements += start to bytes
                }
            }
        }
        val size =
            maxOf(
                declaredLength,
                placements.maxOfOrNull { (start, bytes) -> start + bytes.size } ?: 0
            )
        val buffer = ByteArray(size)
        for ((start, bytes) in placements) bytes.copyInto(buffer, start)
        return Outcome.Success(buffer)
    }

    private fun encodeText(
        payloads: List<Payload>,
        values: Map<String, Any?>
    ): Outcome<ByteArray, ProtocolError> {
        val buffer = mutableListOf<Byte>()
        for (payload in payloads) {
            val resolved = values[payload.name] ?: payload.value
            when (val encoded = PayloadCodecs.forType(payload.type).encode(payload, resolved)) {
                is Outcome.Failure -> return encoded
                is Outcome.Success -> buffer.addAll(encoded.value.toList())
            }
        }
        return Outcome.Success(buffer.toByteArray())
    }

    /**
     * Picks the frame matching the discriminator byte at
     * [FrameSchema.commandIndex]/[FrameSchema.commandId]; a single frame needs no discriminator.
     */
    fun selectFrame(frames: List<FrameSchema>, raw: ByteArray): FrameSchema? {
        if (frames.size <= 1) return frames.firstOrNull()
        return frames.firstOrNull { frame ->
            val index = frame.commandIndex
            val id = frame.commandId
            index != null && id != null && raw.getOrNull(index)?.toInt()?.and(0xFF) == id
        }
    }

    fun decode(response: ResponseSchema, raw: ByteArray): Outcome<DecodedFrame, ProtocolError> {
        val frame =
            selectFrame(response.frames, raw) ?: return Outcome.Success(
                DecodedFrame(
                    emptyMap(),
                    raw
                )
            )
        val values = mutableMapOf<String, Any>()
        for (payload in frame.payloads) {
            val slice =
                when {
                    payload.start == null -> raw
                    payload.end == null -> {
                        val s = resolveIndex(payload.start, raw.size)
                        if (s > raw.size) {
                            return Outcome.Failure(
                                ProtocolError.Decoding(
                                    payload.name,
                                    "start $s beyond frame size ${raw.size}"
                                )
                            )
                        }
                        raw.copyOfRange(s, raw.size)
                    }

                    else -> {
                        val s = resolveIndex(payload.start, raw.size)
                        val e = resolveIndex(payload.end, raw.size)
                        if (e >= raw.size || s > e) {
                            return Outcome.Failure(
                                ProtocolError.Decoding(
                                    payload.name,
                                    "invalid range [$s,$e] for frame size ${raw.size}"
                                )
                            )
                        }
                        raw.copyOfRange(s, e + 1)
                    }
                }
            when (val decoded = PayloadCodecs.forType(payload.type).decode(payload, slice)) {
                is Outcome.Failure -> return decoded
                is Outcome.Success -> values[payload.name] = decoded.value
            }
        }
        return Outcome.Success(DecodedFrame(values, raw))
    }

    /**
     * Negative offsets count from the end of the frame (Kotlin-slice style) — needed for trailers
     * like an APDU's SW1/SW2 anchored to the tail of a variable-length response.
     */
    private fun resolveIndex(index: Int, size: Int): Int = if (index < 0) size + index else index
}
