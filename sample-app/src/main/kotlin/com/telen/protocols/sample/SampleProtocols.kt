package com.telen.protocols.sample

import android.content.Context
import com.telen.protocols.core.model.DeviceProtocol
import com.telen.protocols.core.parser.AssetProtocolSource
import com.telen.protocols.core.parser.ProtocolConfigParser
import com.telen.protocols.transport.ble.BleRouteModule
import com.telen.protocols.transport.socket.SocketRouteModule
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.plus

private val protocolJson =
    Json {
        ignoreUnknownKeys = true
        classDiscriminator = "type"
        serializersModule = SocketRouteModule + BleRouteModule
    }

private val parser = ProtocolConfigParser(protocolJson)

fun loadLedRibbonProtocol(context: Context): DeviceProtocol =
    parser.parse(AssetProtocolSource(context, "led_ribbon.json"))

fun loadMingerProtocol(context: Context): DeviceProtocol =
    parser.parse(AssetProtocolSource(context, "minger.json"))

/**
 * The LED ribbon's CHECKSUM field has no fixed value in `led_ribbon.json` on purpose (see
 * [com.telen.protocols.core.model.Payload]) — computing it is this app's job, not the codec's. The
 * real device expects a plain sum of the preceding bytes, truncated to one byte.
 */
fun ledRibbonChecksum(vararg bytes: Int): Int = bytes.sum() and 0xFF
