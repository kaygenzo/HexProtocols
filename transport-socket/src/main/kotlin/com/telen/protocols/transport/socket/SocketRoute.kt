package com.telen.protocols.transport.socket

import com.telen.protocols.core.model.RouteConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * [address] is nullable because a command's route can be authored without one — some devices are
 * only reachable at an address discovered at runtime (e.g. a broadcast discovery command) rather
 * than one known ahead of time — and supplied later via
 * [com.telen.protocols.core.engine.ProtocolEngine.execute]'s `routeOverride` (`route.copy(address =
 * discoveredIp)`), never by mutating a shared instance.
 */
@Serializable
@SerialName("tcp")
data class TcpRoute(val address: String? = null, val port: Int) : RouteConfig

@Serializable
@SerialName("udp")
data class UdpRoute(val address: String? = null, val port: Int, val isBroadcast: Boolean = false) :
    RouteConfig
