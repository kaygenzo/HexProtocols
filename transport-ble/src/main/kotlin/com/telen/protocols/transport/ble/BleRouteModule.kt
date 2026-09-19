package com.telen.protocols.transport.ble

import com.telen.protocols.core.model.RouteConfig
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

/**
 * Composed into the app's [kotlinx.serialization.json.Json] alongside other transports' modules.
 */
val BleRouteModule = SerializersModule {
    polymorphic(RouteConfig::class) {
        subclass(BleRoute::class, BleRoute.serializer())
    }
}
