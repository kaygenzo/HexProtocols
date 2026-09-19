package com.telen.protocols.transport.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.content.Context
import com.telen.protocols.core.error.Outcome
import com.telen.protocols.core.error.TransportError
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

private val CLIENT_CHARACTERISTIC_CONFIG_UUID = UUID.fromString(
    "00002902-0000-1000-8000-00805f9b34fb"
)

/**
 * Real [GattApi] backed by [android.bluetooth.BluetoothGatt]. Uses the pre-API-33 write/read
 * methods (`setValue` + no-argument `writeCharacteristic`/`writeDescriptor`, deprecated since API
 * 33 in favor of a value-carrying overload) since `minSdk` here is 24 — this is the same tradeoff
 * every BLE library still supporting older devices makes. Not unit-testable (these are Android
 * framework classes with no real behavior outside a device); needs manual/instrumented
 * verification against a real peripheral before shipping.
 *
 * `BLUETOOTH_CONNECT`/`BLUETOOTH_SCAN` (API 31+) are runtime permissions: granting them is the
 * consuming app's responsibility (declared in this module's manifest, requested at runtime by the
 * app), not this library's — hence the `MissingPermission` lint suppression rather than a
 * permission check here, the same tradeoff other BLE libraries make.
 */
@Suppress("DEPRECATION", "MissingPermission")
class AndroidGattApi(private val context: Context) : GattApi {
    private var gatt: BluetoothGatt? = null
    private var pendingConnect: CompletableDeferred<Outcome<Unit, TransportError>>? = null
    private var pendingDiscovery: CompletableDeferred<Outcome<Unit, TransportError>>? = null
    private var pendingWrite: CompletableDeferred<Outcome<Unit, TransportError>>? = null
    private var pendingDescriptorWrite: CompletableDeferred<Outcome<Unit, TransportError>>? = null

    private val _notifications = MutableSharedFlow<GattNotification>(extraBufferCapacity = 32)
    override val notifications: Flow<GattNotification> = _notifications

    private val callback =
        object : BluetoothGattCallback() {
            override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    pendingConnect?.complete(Outcome.Success(Unit))
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    val cause = IllegalStateException("GATT disconnected, status=$status")
                    pendingConnect?.complete(
                        Outcome.Failure(TransportError.ConnectionFailed(cause))
                    )
                }
            }

            override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
                pendingDiscovery?.complete(statusOutcome(status, "discoverServices"))
            }

            override fun onCharacteristicWrite(
                g: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int
            ) {
                pendingWrite?.complete(statusOutcome(status, "writeCharacteristic"))
            }

            override fun onDescriptorWrite(
                g: BluetoothGatt,
                descriptor: BluetoothGattDescriptor,
                status: Int
            ) {
                pendingDescriptorWrite?.complete(statusOutcome(status, "writeDescriptor"))
            }

            override fun onCharacteristicChanged(
                g: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic
            ) {
                _notifications.tryEmit(
                    GattNotification(
                        service = characteristic.service.uuid.toString(),
                        characteristic = characteristic.uuid.toString(),
                        value = characteristic.value ?: ByteArray(0)
                    )
                )
            }
        }

    override suspend fun connect(
        macAddress: String,
        autoBond: Boolean
    ): Outcome<Unit, TransportError> {
        val adapter =
            BluetoothAdapter.getDefaultAdapter()
                ?: return Outcome.Failure(
                    TransportError.ConnectionFailed(IllegalStateException("no Bluetooth adapter"))
                )
        val deferred = CompletableDeferred<Outcome<Unit, TransportError>>()
        pendingConnect = deferred
        gatt = adapter.getRemoteDevice(macAddress).connectGatt(context, autoBond, callback)
        return deferred.await()
    }

    override suspend fun disconnect() {
        gatt?.disconnect()
        gatt?.close()
        gatt = null
    }

    override suspend fun discoverServices(): Outcome<Unit, TransportError> {
        val g = gatt ?: return notConnected()
        val deferred = CompletableDeferred<Outcome<Unit, TransportError>>()
        pendingDiscovery = deferred
        if (!g.discoverServices()) return callFailed("discoverServices")
        return deferred.await()
    }

    override suspend fun writeCharacteristic(
        service: String,
        characteristic: String,
        value: ByteArray,
        writeType: BleWriteType
    ): Outcome<Unit, TransportError> {
        val g = gatt ?: return notConnected()
        val char =
            findCharacteristic(g, service, characteristic)
                ?: return characteristicNotFound(service, characteristic)

        char.writeType =
            when (writeType) {
                BleWriteType.WITH_RESPONSE -> BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                BleWriteType.WITHOUT_RESPONSE -> BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            }
        char.setValue(value)

        val deferred = CompletableDeferred<Outcome<Unit, TransportError>>()
        pendingWrite = deferred
        if (!g.writeCharacteristic(char)) return callFailed("writeCharacteristic")
        return deferred.await()
    }

    override suspend fun enableNotifications(
        service: String,
        characteristic: String,
        mode: BleResponseMode
    ): Outcome<Unit, TransportError> {
        val g = gatt ?: return notConnected()
        val char =
            findCharacteristic(g, service, characteristic)
                ?: return characteristicNotFound(service, characteristic)

        if (!g.setCharacteristicNotification(
                char,
                true
            )
        ) {
            return callFailed("setCharacteristicNotification")
        }
        val descriptor =
            char.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID)
                ?: return Outcome.Failure(
                    TransportError.IOFailure(IllegalStateException("CCCD descriptor not found"))
                )
        descriptor.setValue(
            when (mode) {
                BleResponseMode.NOTIFICATION -> BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                BleResponseMode.INDICATION -> BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            }
        )

        val deferred = CompletableDeferred<Outcome<Unit, TransportError>>()
        pendingDescriptorWrite = deferred
        if (!g.writeDescriptor(descriptor)) return callFailed("writeDescriptor")
        return deferred.await()
    }

    private fun findCharacteristic(
        gatt: BluetoothGatt,
        service: String,
        characteristic: String
    ): BluetoothGattCharacteristic? = gatt.getService(
        UUID.fromString(service)
    )?.getCharacteristic(UUID.fromString(characteristic))

    private fun statusOutcome(status: Int, operation: String): Outcome<Unit, TransportError> =
        if (status == BluetoothGatt.GATT_SUCCESS) {
            Outcome.Success(Unit)
        } else {
            Outcome.Failure(
                TransportError.IOFailure(IllegalStateException("$operation failed, status=$status"))
            )
        }

    private fun notConnected(): Outcome<Unit, TransportError> =
        Outcome.Failure(TransportError.NotConnected("not connected"))

    private fun callFailed(operation: String): Outcome<Unit, TransportError> = Outcome.Failure(
        TransportError.IOFailure(IllegalStateException("$operation() returned false"))
    )

    private fun characteristicNotFound(
        service: String,
        characteristic: String
    ): Outcome<Unit, TransportError> = Outcome.Failure(
        TransportError.IOFailure(
            IllegalStateException("characteristic not found: $service/$characteristic")
        )
    )
}
