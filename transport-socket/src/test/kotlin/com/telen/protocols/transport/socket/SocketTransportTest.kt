package com.telen.protocols.transport.socket

import com.telen.protocols.core.error.Outcome
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.ServerSocket
import kotlin.concurrent.thread
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.jupiter.api.Test

private const val SERVER_TIMEOUT_MILLIS = 3_000

/**
 * No mocks: every case here talks to a real [ServerSocket]/[DatagramSocket] on loopback, no
 * hardware required. The server side runs on a daemon [Thread] with its own socket read timeout —
 * required so a test where nothing ever arrives (e.g. broadcast being dropped by a sandboxed
 * network) fails promptly instead of hanging the JVM forever. Received bytes are captured into a
 * var and asserted on the test's own thread after `join()`, since an assertion failure raised on a
 * bare background thread would otherwise never fail the test.
 */
class SocketTransportTest {
    @Test
    fun `TCP request writes the frame and reads the reply from the same connection`() =
        runBlocking {
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
                            write(byteArrayOf(0x01))
                            flush()
                        }
                    }
                }

            val transport = SocketTransport(readTimeoutMillis = 2_000)
            val route = TcpRoute(address = "127.0.0.1", port = server.localPort)

            val result = transport.send(route, byteArrayOf(0x71, 0x23, 0x0F, 0x9D.toByte()))
            val reply = withTimeoutOrNull(2_000) { transport.observe(route).firstOrNull() }

            serverThread.join(SERVER_TIMEOUT_MILLIS.toLong())
            server.close()

            assertTrue(result is Outcome.Success)
            assertEquals(listOf(0x71, 0x23, 0x0F, 0x9D), received?.map { it.toInt() and 0xFF })
            assertEquals(listOf(0x01.toByte()), reply?.toList())
        }

    @Test
    fun `TCP send with nothing listening fails as a transport error`() = runBlocking {
        val transport = SocketTransport(readTimeoutMillis = 300)
        val result =
            transport.send(
                TcpRoute(address = "127.0.0.1", port = 1),
                byteArrayOf(0x00)
            )
        assertTrue(result is Outcome.Failure)
    }

    @Test
    fun `UDP request sends a unicast datagram and reads the reply on the same socket`() =
        runBlocking {
            val server = DatagramSocket(0)
            var received: String? = null
            val serverThread =
                thread(isDaemon = true) {
                    server.soTimeout = SERVER_TIMEOUT_MILLIS
                    val buffer = ByteArray(64)
                    val packet = DatagramPacket(buffer, buffer.size)
                    server.receive(packet)
                    received = String(packet.data, 0, packet.length, Charsets.US_ASCII)
                    val reply = "10.0.0.42".toByteArray(Charsets.US_ASCII)
                    server.send(DatagramPacket(reply, reply.size, packet.address, packet.port))
                }

            val transport = SocketTransport(readTimeoutMillis = 2_000)
            val route = UdpRoute(address = "127.0.0.1", port = server.localPort)

            val result = transport.send(route, "HF-A11ASSISTHREAD".toByteArray(Charsets.US_ASCII))
            val reply = withTimeoutOrNull(2_000) { transport.observe(route).firstOrNull() }

            serverThread.join(SERVER_TIMEOUT_MILLIS.toLong())
            server.close()

            assertTrue(result is Outcome.Success)
            assertEquals("HF-A11ASSISTHREAD", received)
            assertEquals("10.0.0.42", reply?.let { String(it, Charsets.US_ASCII) })
        }

    @Test
    fun `UDP broadcast reaches a listener bound on the same host`() = runBlocking {
        val listener = DatagramSocket(0)
        var received: String? = null
        val serverThread =
            thread(isDaemon = true) {
                listener.soTimeout = SERVER_TIMEOUT_MILLIS
                val buffer = ByteArray(64)
                val packet = DatagramPacket(buffer, buffer.size)
                listener.receive(packet)
                received = String(packet.data, 0, packet.length, Charsets.US_ASCII)
            }

        val transport = SocketTransport(readTimeoutMillis = 1_000)
        val route = UdpRoute(port = listener.localPort, isBroadcast = true)
        val result = transport.send(route, "HF-A11ASSISTHREAD".toByteArray(Charsets.US_ASCII))

        serverThread.join(SERVER_TIMEOUT_MILLIS.toLong())
        listener.close()

        assertTrue(result is Outcome.Success)
        assertEquals("HF-A11ASSISTHREAD", received)
    }

    @Test
    fun `no reply within the timeout leaves observe empty without failing send`() = runBlocking {
        val server = DatagramSocket(0)
        val transport = SocketTransport(readTimeoutMillis = 300)
        val route = UdpRoute(address = "127.0.0.1", port = server.localPort)

        val result = transport.send(route, byteArrayOf(0x00))
        val reply = withTimeoutOrNull(500) { transport.observe(route).firstOrNull() }

        server.close()
        assertTrue(result is Outcome.Success)
        assertNull(reply)
    }
}
