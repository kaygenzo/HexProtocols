package com.telen.protocols.core.error

sealed interface ProtocolError {
    data class Transport(val error: TransportError) : ProtocolError

    data class Encoding(val payloadName: String, val reason: String) : ProtocolError

    data class Decoding(val payloadName: String, val reason: String) : ProtocolError

    data class UnknownCommand(val identifier: String) : ProtocolError

    data class Configuration(val reason: String) : ProtocolError
}
