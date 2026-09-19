package com.telen.protocols.core.model

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * BINARY: payloads are packed at their [Payload.start]/[Payload.end] byte offsets (BLE/socket
 * fixed-format commands). TEXT: payloads are concatenated in declared order with no offsets (e.g.
 * the AT-command style UDP provisioning frames), made explicit instead of inferred.
 */
enum class FrameLayout { BINARY, TEXT }

object DurationMillisSerializer : KSerializer<Duration> {
    override val descriptor = PrimitiveSerialDescriptor("DurationMillis", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: Duration) =
        encoder.encodeLong(value.inWholeMilliseconds)

    override fun deserialize(decoder: Decoder): Duration = decoder.decodeLong().milliseconds
}

@Serializable
data class RequestSchema(
    val route: RouteConfig,
    val payloads: List<Payload> = emptyList(),
    val layout: FrameLayout = FrameLayout.BINARY,
    val length: Int = 0,
    @Serializable(with = DurationMillisSerializer::class) val timeout: Duration = Duration.ZERO
)
