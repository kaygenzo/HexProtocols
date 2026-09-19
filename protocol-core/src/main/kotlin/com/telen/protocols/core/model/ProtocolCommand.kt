package com.telen.protocols.core.model

import kotlinx.serialization.Serializable

@Serializable
data class ProtocolCommand(
    val identifier: String,
    val request: RequestSchema? = null,
    val response: ResponseSchema? = null
)
