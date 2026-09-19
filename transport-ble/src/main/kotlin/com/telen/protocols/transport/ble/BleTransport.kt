package com.telen.protocols.transport.ble

import com.telen.protocols.core.error.Outcome
import com.telen.protocols.core.error.TransportError
import com.telen.protocols.core.model.RouteConfig
import com.telen.protocols.core.transport.ConnectionState
import com.telen.protocols.core.transport.ConnectionTarget
import com.telen.protocols.core.transport.Transport
import com.telen.protocols.core.transport.TransportId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * BLE [Transport], delegating every real GATT operation to [gatt] (see [GattApi] for why). Unlike
 * [com.telen.protocols.transport.socket.SocketTransport], [observe] never completes on its own — a
 * GATT characteristic can legitimately keep notifying for as long as the connection is held, so a
 * caller collecting a response with no [com.telen.protocols.core.model.ResponseSchema.timeout]
 * needs its own bounded strategy (`.first()`, `.take(n)`, or an explicit schema timeout), the same
 * way it would for any other continuous stream.
 *
 * [connectionState] only reflects what [connect]/[disconnect] set here — [GattApi] has no way to
 * push a spontaneous mid-session disconnect back up yet, a known gap for a later iteration.
 */
class BleTransport(private val gatt: GattApi) : Transport {
    override val id: TransportId = TransportId.BLE

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    override suspend fun connect(target: ConnectionTarget): Outcome<Unit, TransportError> {
        val bleTarget =
            target as? BleTarget
                ?: return Outcome.Failure(
                    TransportError.NotConnected("BleTransport requires a BleTarget")
                )

        _connectionState.value = ConnectionState.Connecting

        val connected = gatt.connect(bleTarget.macAddress, bleTarget.autoBond)
        if (connected is Outcome.Failure) {
            _connectionState.value = ConnectionState.Failed(connected.error)
            return connected
        }

        val discovered = gatt.discoverServices()
        if (discovered is Outcome.Failure) {
            _connectionState.value = ConnectionState.Failed(discovered.error)
            return discovered
        }

        _connectionState.value = ConnectionState.Connected
        return Outcome.Success(Unit)
    }

    override suspend fun disconnect() {
        gatt.disconnect()
        _connectionState.value = ConnectionState.Disconnected
    }

    override suspend fun send(route: RouteConfig, frame: ByteArray): Outcome<Unit, TransportError> {
        val ble =
            route as? BleRoute ?: return Outcome.Failure(TransportError.UnsupportedRoute(route))
        return gatt.writeCharacteristic(ble.service, ble.characteristic, frame, ble.writeType)
    }

    override fun observe(route: RouteConfig): Flow<ByteArray> {
        val ble = route as? BleRoute ?: return emptyFlow()
        return flow {
            val enabled = gatt.enableNotifications(
                ble.service,
                ble.characteristic,
                ble.responseMode
            )
            if (enabled is Outcome.Failure) return@flow
            emitAll(
                gatt.notifications
                    .filter { it.service.equals(ble.service, ignoreCase = true) }
                    .filter { it.characteristic.equals(ble.characteristic, ignoreCase = true) }
                    .map { it.value }
            )
        }
    }
}
