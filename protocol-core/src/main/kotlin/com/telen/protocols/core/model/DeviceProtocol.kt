package com.telen.protocols.core.model

import kotlinx.serialization.Serializable

@Serializable
data class DeviceProtocol(
    val deviceNames: List<String>,
    val commands: List<ProtocolCommand> = emptyList()
) {
    fun command(identifier: String): ProtocolCommand? = commands.firstOrNull {
        it.identifier ==
            identifier
    }
}
