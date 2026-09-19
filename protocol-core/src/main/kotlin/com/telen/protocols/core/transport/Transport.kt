package com.telen.protocols.core.transport

import com.telen.protocols.core.error.Outcome
import com.telen.protocols.core.error.TransportError
import com.telen.protocols.core.model.RouteConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * The one contract every technology (BLE, socket, future APDU/...) implements. [ProtocolEngine]
 * only ever talks to this interface — no transport-specific branching lives above it.
 *
 * `scan()` is deliberately not part of this contract: BLE scanning is genuinely transport-specific
 * (exposed as an optional interface only the BLE module implements), and a device "scan" that is
 * really just a request/response exchange (e.g. a UDP broadcast discovery command) is not a
 * transport primitive at all — it's an ordinary
 * [ProtocolCommand][com.telen.protocols.core.model.ProtocolCommand] executed through the engine
 * like any other.
 */
interface Transport {
    val id: TransportId
    val connectionState: StateFlow<ConnectionState>

    suspend fun connect(target: ConnectionTarget): Outcome<Unit, TransportError>

    suspend fun disconnect()

    suspend fun send(route: RouteConfig, frame: ByteArray): Outcome<Unit, TransportError>

    fun observe(route: RouteConfig): Flow<ByteArray>
}
