package com.telen.protocols.transport.ble

import com.telen.protocols.core.model.RouteConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class BleWriteType { WITH_RESPONSE, WITHOUT_RESPONSE }

enum class BleResponseMode { NOTIFICATION, INDICATION }

@Serializable
@SerialName("ble")
data class BleRoute(
    val service: String,
    val characteristic: String,
    val writeType: BleWriteType = BleWriteType.WITH_RESPONSE,
    val responseMode: BleResponseMode = BleResponseMode.NOTIFICATION
) : RouteConfig
