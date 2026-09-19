package com.telen.protocols.core.codec

import com.telen.protocols.core.error.Outcome
import com.telen.protocols.core.error.ProtocolError
import com.telen.protocols.core.model.ByteOrder
import com.telen.protocols.core.model.Payload
import com.telen.protocols.core.model.PayloadType

/**
 * Encodes/decodes one [PayloadType], enforcing bounds in the same pass instead of two
 * separately-parsed switch statements.
 */
interface PayloadTypeCodec {
    fun encode(payload: Payload, value: Any?): Outcome<ByteArray, ProtocolError>

    fun decode(payload: Payload, bytes: ByteArray): Outcome<Any, ProtocolError>
}

private fun Payload.encodingError(reason: String) =
    Outcome.Failure(ProtocolError.Encoding(name, reason))

private fun Payload.decodingError(reason: String) =
    Outcome.Failure(ProtocolError.Decoding(name, reason))

private fun ByteArray.applyDirection(direction: ByteOrder): ByteArray = if (direction ==
    ByteOrder.RTL
) {
    reversedArray()
} else {
    this
}

private fun hexStringToBytes(hex: String): ByteArray {
    val normalized =
        hex.removePrefix("0x").removePrefix("0X").let { if (it.length % 2 == 1) "0$it" else it }
    return ByteArray(normalized.length / 2) { i ->
        normalized.substring(i * 2, i * 2 + 2).toInt(16).toByte()
    }
}

private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

private fun intToBytes(value: Long, size: Int): ByteArray =
    ByteArray(size) { i -> (value shr ((size - 1 - i) * 8)).toByte() }

private fun bytesToLong(bytes: ByteArray): Long = bytes.fold(0L) { acc, byte ->
    (acc shl 8) or
        (byte.toLong() and 0xFF)
}

object HexCodec : PayloadTypeCodec {
    override fun encode(payload: Payload, value: Any?): Outcome<ByteArray, ProtocolError> {
        val raw = value ?: return payload.encodingError("missing value")
        val bytes =
            try {
                when (raw) {
                    is Number -> {
                        var hex = raw.toLong().toString(16)
                        if (hex.length % 2 == 1) hex = "0$hex"
                        hexStringToBytes(hex)
                    }

                    is String -> hexStringToBytes(raw)
                    else -> return payload.encodingError(
                        "unsupported HEX value type ${raw::class.simpleName}"
                    )
                }
            } catch (e: NumberFormatException) {
                return payload.encodingError("invalid HEX value '$raw'")
            }
        return Outcome.Success(bytes.applyDirection(payload.direction))
    }

    override fun decode(payload: Payload, bytes: ByteArray): Outcome<Any, ProtocolError> =
        Outcome.Success(
            "0x" + bytes.applyDirection(payload.direction).toHex()
        )
}

object HexStringCodec : PayloadTypeCodec {
    override fun encode(payload: Payload, value: Any?): Outcome<ByteArray, ProtocolError> {
        val raw =
            value as? String ?: return payload.encodingError("HEX_STRING requires a String value")
        return try {
            Outcome.Success(hexStringToBytes(raw).applyDirection(payload.direction))
        } catch (e: NumberFormatException) {
            payload.encodingError("invalid HEX_STRING value '$raw'")
        }
    }

    override fun decode(payload: Payload, bytes: ByteArray): Outcome<Any, ProtocolError> =
        Outcome.Success(bytes.applyDirection(payload.direction).toHex())
}

object StringCodec : PayloadTypeCodec {
    override fun encode(payload: Payload, value: Any?): Outcome<ByteArray, ProtocolError> {
        val raw = value as? String ?: return payload.encodingError("STRING requires a String value")
        return Outcome.Success(raw.toByteArray(Charsets.UTF_8).applyDirection(payload.direction))
    }

    override fun decode(payload: Payload, bytes: ByteArray): Outcome<Any, ProtocolError> =
        Outcome.Success(String(bytes.applyDirection(payload.direction), Charsets.UTF_8))
}

object AsciiCodec : PayloadTypeCodec {
    override fun encode(payload: Payload, value: Any?): Outcome<ByteArray, ProtocolError> {
        val raw = value?.toString() ?: return payload.encodingError("ASCII requires a value")
        return Outcome.Success(raw.toByteArray(Charsets.US_ASCII))
    }

    override fun decode(payload: Payload, bytes: ByteArray): Outcome<Any, ProtocolError> =
        Outcome.Success(String(bytes, Charsets.US_ASCII))
}

object IntegerCodec : PayloadTypeCodec {
    override fun encode(payload: Payload, value: Any?): Outcome<ByteArray, ProtocolError> {
        val raw = value ?: return payload.encodingError("missing value")
        val intValue =
            when (raw) {
                is Int -> raw
                is Number -> raw.toInt()
                is String ->
                    raw.toIntOrNull()
                        ?: return payload.encodingError("invalid INTEGER value '$raw'")

                else -> return payload.encodingError(
                    "unsupported INTEGER value type ${raw::class.simpleName}"
                )
            }
        payload.min?.let { min ->
            if (intValue <
                min.toInt()
            ) {
                return payload.encodingError("$intValue below min $min")
            }
        }
        payload.max?.let { max ->
            if (intValue >
                max.toInt()
            ) {
                return payload.encodingError("$intValue above max $max")
            }
        }
        val size =
            if (payload.start != null &&
                payload.end != null
            ) {
                (payload.end - payload.start + 1)
            } else {
                1
            }
        return Outcome.Success(
            intToBytes(
                intValue.toLong(),
                size
            ).applyDirection(payload.direction)
        )
    }

    override fun decode(payload: Payload, bytes: ByteArray): Outcome<Any, ProtocolError> {
        val value = bytesToLong(bytes.applyDirection(payload.direction)).toInt()
        payload.min?.let { min ->
            if (value <
                min.toInt()
            ) {
                return payload.decodingError("$value below min $min")
            }
        }
        payload.max?.let { max ->
            if (value >
                max.toInt()
            ) {
                return payload.decodingError("$value above max $max")
            }
        }
        return Outcome.Success(value)
    }
}

object LongCodec : PayloadTypeCodec {
    override fun encode(payload: Payload, value: Any?): Outcome<ByteArray, ProtocolError> {
        val raw = value ?: return payload.encodingError("missing value")
        val longValue =
            when (raw) {
                is Long -> raw
                is Number -> raw.toLong()
                is String ->
                    raw.toLongOrNull()
                        ?: return payload.encodingError("invalid LONG value '$raw'")

                else -> return payload.encodingError(
                    "unsupported LONG value type ${raw::class.simpleName}"
                )
            }
        val size =
            if (payload.start != null &&
                payload.end != null
            ) {
                (payload.end - payload.start + 1)
            } else {
                8
            }
        return Outcome.Success(intToBytes(longValue, size).applyDirection(payload.direction))
    }

    override fun decode(payload: Payload, bytes: ByteArray): Outcome<Any, ProtocolError> =
        Outcome.Success(bytesToLong(bytes.applyDirection(payload.direction)))
}

object PayloadCodecs {
    private val registry: Map<PayloadType, PayloadTypeCodec> =
        mapOf(
            PayloadType.HEX to HexCodec,
            PayloadType.HEX_STRING to HexStringCodec,
            PayloadType.STRING to StringCodec,
            PayloadType.ASCII to AsciiCodec,
            PayloadType.INTEGER to IntegerCodec,
            PayloadType.LONG to LongCodec
        )

    fun forType(type: PayloadType): PayloadTypeCodec = registry.getValue(type)
}
