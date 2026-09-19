package com.telen.protocols.core.model

import kotlinx.serialization.Serializable

/**
 * One possible shape of an incoming frame; [commandId]/[commandIndex] disambiguate it from others
 * on the same route.
 */
@Serializable
data class FrameSchema(
    val payloads: List<Payload> = emptyList(),
    val commandId: Int? = null,
    val commandIndex: Int? = null
)
