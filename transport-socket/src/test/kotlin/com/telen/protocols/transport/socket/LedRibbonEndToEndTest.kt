package com.telen.protocols.transport.socket

import com.telen.protocols.core.engine.ProtocolEngine
import com.telen.protocols.core.error.Outcome
import com.telen.protocols.core.parser.ProtocolConfigParser
import com.telen.protocols.core.parser.ProtocolSource
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.ServerSocket
import kotlin.concurrent.thread
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

private const val SERVER_TIMEOUT_MILLIS = 3_000

/**
 * Drives the real `led_ribbon.json` fixture end to end through [ProtocolConfigParser] +
 * [ProtocolEngine] + [SocketTransport] — the Phase 3 milestone: no mocks, no hardware, loopback
 * TCP/UDP servers standing in for the LED ribbon. Server-side threads are daemons with their own
 * socket read timeout, so a test never hangs the JVM if something doesn't arrive; their assertions
 * are captured into a var and checked on the test thread after `join()`, since a failure raised on
 * a bare background thread would otherwise never fail the test.
 */
class LedRibbonEndToEndTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            classDiscriminator = "type"
            serializersModule = SocketRouteModule
        }
    private val protocol =
        ProtocolConfigParser(json).parse(
            ProtocolSource {
                requireNotNull(javaClass.classLoader).getResourceAsStream("led_ribbon.json")!!
            }
        )

    @Test
    fun `LIGHT_ON writes the fixed command bytes over TCP and decodes the raw ack`() = runBlocking {
        val server = ServerSocket(0)
        var received: ByteArray? = null
        val serverThread =
            thread(isDaemon = true) {
                server.soTimeout = SERVER_TIMEOUT_MILLIS
                server.accept().use { client ->
                    val buffer = ByteArray(4)
                    client.getInputStream().read(buffer)
                    received = buffer
                    client.getOutputStream().apply {
                        write(byteArrayOf(0x4F, 0x4B))
                        flush()
                    }
                }
            }

        val command = requireNotNull(protocol.command("LIGHT_ON"))
        val route = TcpRoute(address = "127.0.0.1", port = server.localPort)
        val engine = ProtocolEngine(SocketTransport(readTimeoutMillis = 2_000))

        val results =
            engine
                .execute(command, values = mapOf("CHECKSUM" to 0x9D), routeOverride = route)
                .toList()

        serverThread.join(SERVER_TIMEOUT_MILLIS.toLong())
        server.close()

        assertEquals(listOf(0x71, 0x23, 0x0F, 0x9D), received?.map { it.toInt() and 0xFF })
        val success = results.single() as Outcome.Success
        assertEquals("OK", String(success.value.raw, Charsets.US_ASCII))
    }

    @Test
    fun `CHANGE_COLOR is fire-and-forget and never touches observe`() = runBlocking {
        val server = ServerSocket(0)
        var received: ByteArray? = null
        val serverThread =
            thread(isDaemon = true) {
                server.soTimeout = SERVER_TIMEOUT_MILLIS
                server.accept().use { client ->
                    val buffer = ByteArray(8)
                    client.getInputStream().read(buffer)
                    received = buffer
                }
            }

        val command = requireNotNull(protocol.command("CHANGE_COLOR"))
        val route = TcpRoute(address = "127.0.0.1", port = server.localPort)
        val engine = ProtocolEngine(SocketTransport(readTimeoutMillis = 2_000))

        val results =
            engine
                .execute(
                    command,
                    values =
                    mapOf(
                        "RED" to 255,
                        "GREEN" to 0,
                        "BLUE" to 128,
                        "CHECKSUM" to 0x2E
                    ),
                    routeOverride = route
                ).toList()

        serverThread.join(SERVER_TIMEOUT_MILLIS.toLong())
        server.close()

        assertEquals(
            listOf(0x31, 255, 0, 128, 0, 0, 0x0F, 0x2E),
            received?.map { it.toInt() and 0xFF }
        )
        assertTrue(results.isEmpty())
    }

    @Test
    fun `GET_REMOTE_ADDRESS then SEND_SSID reuses the discovered address via routeOverride`() =
        runBlocking {
            // The discovery reply is the loopback address itself, so the second, independently
            // bound server below is genuinely reachable through the very address this test
            // resolves at runtime and passes on as an immutable `routeOverride` (never a mutation
            // of shared schema state, unlike the old Java LightRibbon.java).
            val discovery = DatagramSocket(0)
            var discoveryReceived: String? = null
            val discoveryThread =
                thread(isDaemon = true) {
                    discovery.soTimeout = SERVER_TIMEOUT_MILLIS
                    val buffer = ByteArray(64)
                    val packet = DatagramPacket(buffer, buffer.size)
                    discovery.receive(packet)
                    discoveryReceived = String(packet.data, 0, packet.length, Charsets.US_ASCII)
                    val reply = "127.0.0.1".toByteArray(Charsets.US_ASCII)
                    discovery.send(DatagramPacket(reply, reply.size, packet.address, packet.port))
                }

            val engine = ProtocolEngine(SocketTransport(readTimeoutMillis = 2_000))
            val discoverCommand = requireNotNull(protocol.command("GET_REMOTE_ADDRESS"))
            val discoverRoute = UdpRoute(address = "127.0.0.1", port = discovery.localPort)

            val discovered =
                engine
                    .execute(discoverCommand, routeOverride = discoverRoute)
                    .toList()
                    .single() as Outcome.Success
            val discoveredAddress = String(discovered.value.raw, Charsets.US_ASCII)

            discoveryThread.join(SERVER_TIMEOUT_MILLIS.toLong())
            discovery.close()
            assertEquals("HF-A11ASSISTHREAD", discoveryReceived)
            assertEquals("127.0.0.1", discoveredAddress)

            val provisioning = DatagramSocket(0)
            var provisioningReceived: String? = null
            val provisioningThread =
                thread(isDaemon = true) {
                    provisioning.soTimeout = SERVER_TIMEOUT_MILLIS
                    val buffer = ByteArray(64)
                    val packet = DatagramPacket(buffer, buffer.size)
                    provisioning.receive(packet)
                    provisioningReceived = String(packet.data, 0, packet.length, Charsets.US_ASCII)
                }

            val sendSsid = requireNotNull(protocol.command("SEND_SSID"))
            val provisioningRoute =
                UdpRoute(address = discoveredAddress, port = provisioning.localPort)
            engine
                .execute(
                    sendSsid,
                    values = mapOf("SSID" to "MyWifi"),
                    routeOverride = provisioningRoute
                ).toList()

            provisioningThread.join(SERVER_TIMEOUT_MILLIS.toLong())
            provisioning.close()
            assertEquals("AT+WSSSID=MyWifi\r", provisioningReceived)
        }
}
