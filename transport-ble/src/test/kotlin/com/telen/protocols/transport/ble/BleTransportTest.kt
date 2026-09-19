package com.telen.protocols.transport.ble

import com.telen.protocols.core.error.Outcome
import com.telen.protocols.core.error.TransportError
import com.telen.protocols.core.transport.ConnectionState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/**
 * No Android framework classes involved: [GattApi] is faked directly with MockK, per the plan's
 * testing strategy.
 */
class BleTransportTest {
    @Test
    fun `connect discovers services and moves to Connected`() = runTest {
        val gatt = mockk<GattApi>()
        coEvery { gatt.connect("AA:BB", false) } returns Outcome.Success(Unit)
        coEvery { gatt.discoverServices() } returns Outcome.Success(Unit)
        val transport = BleTransport(gatt)

        val result = transport.connect(BleTarget(macAddress = "AA:BB"))

        assertTrue(result is Outcome.Success)
        assertEquals(ConnectionState.Connected, transport.connectionState.value)
    }

    @Test
    fun `a failed connect surfaces as a Failed state without discovering services`() = runTest {
        val gatt = mockk<GattApi>()
        val error = TransportError.ConnectionFailed(IllegalStateException("nope"))
        coEvery { gatt.connect(any(), any()) } returns Outcome.Failure(error)
        val transport = BleTransport(gatt)

        val result = transport.connect(BleTarget(macAddress = "AA:BB"))

        assertTrue(result is Outcome.Failure)
        assertEquals(ConnectionState.Failed(error), transport.connectionState.value)
        coVerify(exactly = 0) { gatt.discoverServices() }
    }

    @Test
    fun `send writes the frame to the route's service and characteristic`() = runTest {
        val gatt = mockk<GattApi>()
        coEvery {
            gatt.writeCharacteristic("svc", "chr", any(), BleWriteType.WITH_RESPONSE)
        } returns Outcome.Success(Unit)
        val transport = BleTransport(gatt)
        val route = BleRoute(service = "svc", characteristic = "chr")

        val result = transport.send(route, byteArrayOf(0x01, 0x02))

        assertTrue(result is Outcome.Success)
        coVerify {
            gatt.writeCharacteristic(
                "svc",
                "chr",
                byteArrayOf(0x01, 0x02),
                BleWriteType.WITH_RESPONSE
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `observe enables notifications and filters by the route's characteristic`() = runTest {
        val gatt = mockk<GattApi>()
        val notifications = MutableSharedFlow<GattNotification>(extraBufferCapacity = 4)
        every { gatt.notifications } returns notifications
        coEvery { gatt.enableNotifications("svc", "chr", BleResponseMode.NOTIFICATION) } returns
            Outcome.Success(Unit)
        val transport = BleTransport(gatt)
        val route = BleRoute(service = "svc", characteristic = "chr")

        val received = mutableListOf<ByteArray>()
        val job = launch { transport.observe(route).collect { received += it } }
        advanceUntilIdle()

        notifications.emit(GattNotification("other-service", "chr", byteArrayOf(0x00)))
        notifications.emit(GattNotification("svc", "other-characteristic", byteArrayOf(0x00)))
        notifications.emit(GattNotification("svc", "chr", byteArrayOf(0x42)))
        advanceUntilIdle()
        job.cancel()

        assertEquals(listOf(listOf(0x42.toByte())), received.map { it.toList() })
    }
}
