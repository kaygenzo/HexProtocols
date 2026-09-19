package com.telen.protocols.transport.ble

import com.telen.protocols.core.engine.ProtocolEngine
import com.telen.protocols.core.error.Outcome
import com.telen.protocols.core.parser.ProtocolConfigParser
import com.telen.protocols.core.parser.ProtocolSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

/**
 * Drives the real `minger.json` fixture through [ProtocolConfigParser] + [ProtocolEngine] +
 * [BleTransport] — the BLE analogue of `transport-socket`'s `LedRibbonEndToEndTest`. There is no
 * real Bluetooth radio available in this environment (an Android emulator's Bluetooth stack
 * doesn't support real GATT peripherals either), so [GattApi] is faked instead of exercising
 * [AndroidGattApi]: this proves the schema/codec/engine wiring end to end, not the real radio path,
 * which still needs manual/instrumented verification against a real Minger P50.
 */
class MingerEndToEndTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            classDiscriminator = "type"
            serializersModule = BleRouteModule
        }
    private val protocol =
        ProtocolConfigParser(json).parse(
            ProtocolSource {
                requireNotNull(javaClass.classLoader).getResourceAsStream("minger.json")!!
            }
        )

    @Test
    fun `CHANGE_COLOR writes the fixed 20-byte frame to the device's service and characteristic`() =
        runBlocking {
            val gatt = mockk<GattApi>()
            coEvery {
                gatt.writeCharacteristic(any(), any(), any(), BleWriteType.WITH_RESPONSE)
            } returns Outcome.Success(Unit)

            val command = requireNotNull(protocol.command("CHANGE_COLOR"))
            val engine = ProtocolEngine(BleTransport(gatt))

            val results = engine.execute(command).toList()

            val expectedFrame =
                byteArrayOf(
                    0x01, 0xFE.toByte(), 0x00, 0x00, 0x53, 0x83.toByte(), 0x10, 0x00,
                    0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(),
                    0x00, 0x50,
                    0x02, 0x02,
                    0x00,
                    0x00, 0x00, 0x00, 0x00
                )
            coVerify {
                gatt.writeCharacteristic(
                    "00007777-0000-1000-8000-00805f9b34fb",
                    "00008877-0000-1000-8000-00805f9b34fb",
                    expectedFrame,
                    BleWriteType.WITH_RESPONSE
                )
            }
            assertTrue(results.isEmpty())
        }

    @Test
    fun `CHANGE_COLOR overrides the RGB and luminosity defaults from the values map`() =
        runBlocking {
            val gatt = mockk<GattApi>()
            coEvery {
                gatt.writeCharacteristic(any(), any(), any(), BleWriteType.WITH_RESPONSE)
            } returns Outcome.Success(Unit)

            val command = requireNotNull(protocol.command("CHANGE_COLOR"))
            val engine = ProtocolEngine(BleTransport(gatt))

            engine
                .execute(
                    command,
                    values = mapOf(
                        "RED" to 10,
                        "GREEN" to 20,
                        "BLUE" to 30,
                        "LUMINOSITY_1" to 5,
                        "LUMINOSITY_2" to 5
                    )
                ).toList()

            val expectedFrame =
                byteArrayOf(
                    0x01, 0xFE.toByte(), 0x00, 0x00, 0x53, 0x83.toByte(), 0x10, 0x00,
                    20, 30, 10,
                    0x00, 0x50,
                    5, 5,
                    0x00,
                    0x00, 0x00, 0x00, 0x00
                )
            coVerify {
                gatt.writeCharacteristic(
                    "00007777-0000-1000-8000-00805f9b34fb",
                    "00008877-0000-1000-8000-00805f9b34fb",
                    expectedFrame,
                    BleWriteType.WITH_RESPONSE
                )
            }
        }
}
