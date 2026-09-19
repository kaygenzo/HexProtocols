package com.telen.protocols.transport.ble

import com.telen.protocols.core.error.Outcome
import com.telen.protocols.core.error.TransportError
import kotlinx.coroutines.flow.Flow

data class GattNotification(val service: String, val characteristic: String, val value: ByteArray)

/**
 * The seam between [BleTransport] and the real Android GATT API. [AndroidGattApi] is the real
 * implementation; tests fake this interface directly (with MockK) instead of mocking
 * `BluetoothGatt`/`BluetoothGattCallback`, which are Android framework classes with no real
 * behavior outside a device (the old repo mocked them via PowerMock — this design makes that
 * unnecessary).
 */
interface GattApi {
    val notifications: Flow<GattNotification>

    suspend fun connect(macAddress: String, autoBond: Boolean): Outcome<Unit, TransportError>

    suspend fun disconnect()

    suspend fun discoverServices(): Outcome<Unit, TransportError>

    suspend fun writeCharacteristic(
        service: String,
        characteristic: String,
        value: ByteArray,
        writeType: BleWriteType
    ): Outcome<Unit, TransportError>

    suspend fun enableNotifications(
        service: String,
        characteristic: String,
        mode: BleResponseMode
    ): Outcome<Unit, TransportError>
}
