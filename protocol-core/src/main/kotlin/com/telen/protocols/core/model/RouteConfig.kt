package com.telen.protocols.core.model

import kotlinx.serialization.Polymorphic

/**
 * Marker for "how to address this specific command" (BLE service/characteristic, socket host/port,
 * APDU channel/AID, ...). Deliberately NOT a sealed type: each transport module owns and registers
 * its own [RouteConfig] implementation, so adding a transport never touches this module or any
 * other transport module.
 */
@Polymorphic
interface RouteConfig
