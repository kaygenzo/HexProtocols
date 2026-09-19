package com.telen.protocols.core.transport

import com.telen.protocols.core.error.TransportError

enum class TransportId { BLE, SOCKET, APDU }

sealed interface ConnectionState {
    data object Disconnected : ConnectionState

    data object Connecting : ConnectionState

    data object Connected : ConnectionState

    data class Failed(val error: TransportError) : ConnectionState
}
