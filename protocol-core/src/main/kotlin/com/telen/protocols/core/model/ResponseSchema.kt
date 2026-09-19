package com.telen.protocols.core.model

import kotlin.time.Duration
import kotlinx.serialization.Serializable

@Serializable
data class ResponseSchema(
    val route: RouteConfig,
    val frames: List<FrameSchema> = emptyList(),
    val endFrame: String? = null,
    val completeOnTimeout: Boolean = false,
    @Serializable(with = DurationMillisSerializer::class) val timeout: Duration = Duration.ZERO
)
