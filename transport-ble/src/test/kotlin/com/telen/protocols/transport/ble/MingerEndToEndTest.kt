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
 * A synthetic device protocol modeled on a real BLE RGB controller's wire format (a fixed 20-byte
 * frame with a HEX_STRING prefix/suffix and INTEGER RGB/luminosity fields) — not the config any
 * real app ships, which is deliberately not this library's concern: config files are the calling
 * app's job (see `sample-app`'s assets).
 */
private val syntheticRgbProtocolJson =
    """
    {
      "deviceNames": ["SYNTH-RGB"],
      "commands": [
        {
          "identifier": "CHANGE_COLOR",
          "request": {
            "route": {
              "type": "ble",
              "service": "00007777-0000-1000-8000-00805f9b34fb",
              "characteristic": "00008877-0000-1000-8000-00805f9b34fb"
            },
            "length": 20,
            "payloads": [
              { "name": "PREFIX", "start": 0, "end": 7, "type": "HEX_STRING", "value": "01fe000053831000" },
              { "name": "GREEN", "start": 8, "end": 8, "type": "INTEGER", "min": 0, "max": 255, "value": 255 },
              { "name": "BLUE", "start": 9, "end": 9, "type": "INTEGER", "min": 0, "max": 255, "value": 255 },
              { "name": "RED", "start": 10, "end": 10, "type": "INTEGER", "min": 0, "max": 255, "value": 255 },
              { "name": "UNKNOWN", "start": 11, "end": 12, "type": "HEX_STRING", "value": "0050" },
              { "name": "LUMINOSITY_1", "start": 13, "end": 13, "type": "INTEGER", "min": 0, "max": 255, "value": 2 },
              { "name": "LUMINOSITY_2", "start": 14, "end": 14, "type": "INTEGER", "min": 0, "max": 255, "value": 2 },
              { "name": "SUFFIX", "start": 15, "end": 15, "type": "HEX_STRING", "value": "00" }
            ]
          }
        }
      ]
    }
    """.trimIndent()

/**
 * Drives the synthetic RGB-controller-style protocol above through [ProtocolConfigParser] +
 * [ProtocolEngine] + [BleTransport] — the BLE analogue of `transport-socket`'s
 * `LedRibbonEndToEndTest`. There is no real Bluetooth radio available in this environment (an
 * Android emulator's Bluetooth stack doesn't support real GATT peripherals either), so [GattApi]
 * is faked instead of exercising [AndroidGattApi]: this proves the schema/codec/engine wiring end
 * to end, not the real radio path, which still needs manual/instrumented verification against a
 * real device.
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
                syntheticRgbProtocolJson.byteInputStream()
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
                    values =
                    mapOf(
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
