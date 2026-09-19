package com.telen.protocols.transport.ble

import com.telen.protocols.core.transport.ConnectionTarget

data class BleTarget(val macAddress: String, val autoBond: Boolean = false) : ConnectionTarget
