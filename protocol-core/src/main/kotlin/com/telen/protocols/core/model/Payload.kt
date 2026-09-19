package com.telen.protocols.core.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.jsonPrimitive

enum class PayloadType { HEX, HEX_STRING, STRING, INTEGER, LONG, ASCII }

enum class ByteOrder { LTR, RTL }

/**
 * Protocol authors write payload values/bounds as either a JSON string ("0x71") or a JSON number
 * (255) interchangeably. This reads either literal kind and keeps its raw text form, so
 * [Payload.value]/[Payload.min]/[Payload.max] don't need two representations.
 */
object FlexibleStringSerializer : KSerializer<String> {
    override val descriptor = PrimitiveSerialDescriptor("FlexibleString", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: String) = encoder.encodeString(value)

    override fun deserialize(decoder: Decoder): String {
        val jsonDecoder = decoder as? JsonDecoder ?: return decoder.decodeString()
        return jsonDecoder.decodeJsonElement().jsonPrimitive.content
    }
}

@Serializable
data class Payload(
    val name: String,
    val start: Int? = null,
    val end: Int? = null,
    val type: PayloadType,
    val direction: ByteOrder = ByteOrder.LTR,
    @Serializable(with = FlexibleStringSerializer::class) val value: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) val min: String? = null,
    @Serializable(with = FlexibleStringSerializer::class) val max: String? = null
)
