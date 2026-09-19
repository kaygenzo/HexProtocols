package com.telen.protocols.core.error

import com.telen.protocols.core.model.RouteConfig

sealed interface TransportError {
    data class ConnectionFailed(val cause: Throwable) : TransportError

    data class NotConnected(val message: String) : TransportError

    data class Timeout(val afterMillis: Long) : TransportError

    data class IOFailure(val cause: Throwable) : TransportError

    data class UnsupportedRoute(val route: RouteConfig) : TransportError
}
