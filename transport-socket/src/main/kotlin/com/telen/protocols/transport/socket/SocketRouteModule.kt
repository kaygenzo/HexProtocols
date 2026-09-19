package com.telen.protocols.transport.socket

import com.telen.protocols.core.model.RouteConfig
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

/**
 * Composed into the app's [kotlinx.serialization.json.Json] alongside other transports' modules;
 * protocol-core never sees this.
 */
val SocketRouteModule = SerializersModule {
    polymorphic(RouteConfig::class) {
        subclass(TcpRoute::class, TcpRoute.serializer())
        subclass(UdpRoute::class, UdpRoute.serializer())
    }
}
