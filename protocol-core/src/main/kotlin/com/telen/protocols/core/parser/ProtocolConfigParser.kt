package com.telen.protocols.core.parser

import com.telen.protocols.core.model.DeviceProtocol
import kotlinx.serialization.json.Json

/**
 * Takes an already-composed [Json] instance (with each transport module's
 * [com.telen.protocols.core.model.RouteConfig] subclasses registered into its
 * [kotlinx.serialization.modules.SerializersModule]) — this class never imports a concrete route
 * implementation itself.
 */
class ProtocolConfigParser(private val json: Json) {
    fun parse(source: ProtocolSource): DeviceProtocol = source.open().use { stream ->
        json.decodeFromString(DeviceProtocol.serializer(), stream.readBytes().decodeToString())
    }
}
