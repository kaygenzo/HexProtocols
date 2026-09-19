package com.telen.protocols.transport.socket

import com.telen.protocols.core.error.Outcome
import com.telen.protocols.core.error.TransportError
import com.telen.protocols.core.model.RouteConfig
import com.telen.protocols.core.transport.ConnectionState
import com.telen.protocols.core.transport.ConnectionTarget
import com.telen.protocols.core.transport.Transport
import com.telen.protocols.core.transport.TransportId
import java.io.IOException
import java.io.InputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.withContext

private const val BROADCAST_ADDRESS = "255.255.255.255"
private const val RECEIVE_BUFFER_SIZE = 2048

/**
 * TCP/UDP [Transport]. Every command opens (and closes) its own socket, matching how the LED
 * ribbon protocol actually behaves (short-lived TCP connections, connectionless UDP) — there is no
 * persistent link to hold, so [connect]/[disconnect] only track [connectionState] for API
 * consistency with transports that do have one (e.g. BLE).
 *
 * A response is read opportunistically right after writing, within [send] itself, using
 * [readTimeoutMillis] as the socket's read timeout, and published to a shared flow that [observe]
 * exposes as its next single value. This matches how
 * [com.telen.protocols.core.engine.ProtocolEngine] actually calls this contract: it builds the
 * `observe(route)` flow before awaiting `send(...)`, but only starts collecting it afterwards — a
 * `replay = 1` buffer means the already-read response is still there when that collection starts,
 * regardless of exact timing. `observe` completes after that one value (`take(1)`) rather than
 * staying open forever like a real notification stream would — a socket exchange is one request,
 * one reply, never a continuous subscription — which is what lets `engine.execute(...).toList()`
 * terminate instead of waiting on a flow with no natural end. Commands with no response schema
 * never call [observe], so the read is skipped there too — wasted latency, not a correctness bug.
 */
class SocketTransport(private val readTimeoutMillis: Int = 3_000) : Transport {
    override val id: TransportId = TransportId.SOCKET

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val responses = MutableSharedFlow<ByteArray>(replay = 1, extraBufferCapacity = 16)

    override suspend fun connect(target: ConnectionTarget): Outcome<Unit, TransportError> {
        _connectionState.value = ConnectionState.Connected
        return Outcome.Success(Unit)
    }

    override suspend fun disconnect() {
        _connectionState.value = ConnectionState.Disconnected
    }

    override suspend fun send(route: RouteConfig, frame: ByteArray): Outcome<Unit, TransportError> =
        withContext(Dispatchers.IO) {
            when (route) {
                is TcpRoute -> sendTcp(route, frame)
                is UdpRoute -> sendUdp(route, frame)
                else -> Outcome.Failure(TransportError.UnsupportedRoute(route))
            }
        }

    override fun observe(route: RouteConfig): Flow<ByteArray> = responses.take(1)

    private fun sendTcp(route: TcpRoute, frame: ByteArray): Outcome<Unit, TransportError> {
        val address =
            route.address
                ?: return Outcome.Failure(TransportError.UnsupportedRoute(route))
        return try {
            Socket().use { socket ->
                socket.soTimeout = readTimeoutMillis
                socket.connect(InetSocketAddress(address, route.port), readTimeoutMillis)
                socket.getOutputStream().let {
                    it.write(frame)
                    it.flush()
                }
                readAvailable(socket.getInputStream())?.let(responses::tryEmit)
            }
            Outcome.Success(Unit)
        } catch (e: IOException) {
            Outcome.Failure(TransportError.IOFailure(e))
        }
    }

    private fun sendUdp(route: UdpRoute, frame: ByteArray): Outcome<Unit, TransportError> {
        val address =
            (if (route.isBroadcast) BROADCAST_ADDRESS else route.address)
                ?: return Outcome.Failure(TransportError.UnsupportedRoute(route))
        return try {
            DatagramSocket().use { socket ->
                socket.broadcast = route.isBroadcast
                socket.soTimeout = readTimeoutMillis
                socket.send(
                    DatagramPacket(frame, frame.size, InetAddress.getByName(address), route.port)
                )
                readReply(socket)?.let(responses::tryEmit)
            }
            Outcome.Success(Unit)
        } catch (e: IOException) {
            Outcome.Failure(TransportError.IOFailure(e))
        }
    }

    private fun readAvailable(input: InputStream): ByteArray? = try {
        val buffer = ByteArray(RECEIVE_BUFFER_SIZE)
        val read = input.read(buffer)
        if (read > 0) buffer.copyOf(read) else null
    } catch (e: SocketTimeoutException) {
        null
    }

    private fun readReply(socket: DatagramSocket): ByteArray? = try {
        val buffer = ByteArray(RECEIVE_BUFFER_SIZE)
        val packet = DatagramPacket(buffer, buffer.size)
        socket.receive(packet)
        packet.data.copyOf(packet.length)
    } catch (e: SocketTimeoutException) {
        null
    }
}
