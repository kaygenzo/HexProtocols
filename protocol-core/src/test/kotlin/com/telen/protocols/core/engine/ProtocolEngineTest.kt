package com.telen.protocols.core.engine

import com.telen.protocols.core.codec.DecodedFrame
import com.telen.protocols.core.error.Outcome
import com.telen.protocols.core.error.ProtocolError
import com.telen.protocols.core.error.TransportError
import com.telen.protocols.core.model.FrameSchema
import com.telen.protocols.core.model.Payload
import com.telen.protocols.core.model.PayloadType
import com.telen.protocols.core.model.ProtocolCommand
import com.telen.protocols.core.model.RequestSchema
import com.telen.protocols.core.model.ResponseSchema
import com.telen.protocols.core.model.RouteConfig
import com.telen.protocols.core.transport.ConnectionState
import com.telen.protocols.core.transport.ConnectionTarget
import com.telen.protocols.core.transport.Transport
import com.telen.protocols.core.transport.TransportId
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

private object FakeRoute : RouteConfig

private class FakeTransport(
    private val sendResult: Outcome<Unit, TransportError> = Outcome.Success(Unit)
) : Transport {
    override val id: TransportId = TransportId.SOCKET
    override val connectionState: StateFlow<ConnectionState> =
        MutableStateFlow(ConnectionState.Connected)
    val sentFrames = mutableListOf<ByteArray>()
    val incoming = MutableSharedFlow<ByteArray>(replay = 1, extraBufferCapacity = 4)

    override suspend fun connect(target: ConnectionTarget) = Outcome.Success(Unit)

    override suspend fun disconnect() = Unit

    override suspend fun send(route: RouteConfig, frame: ByteArray): Outcome<Unit, TransportError> {
        sentFrames += frame
        return sendResult
    }

    override fun observe(route: RouteConfig): Flow<ByteArray> = incoming
}

class ProtocolEngineTest {
    private val pingCommand =
        ProtocolCommand(
            identifier = "PING",
            request =
            RequestSchema(
                route = FakeRoute,
                length = 1,
                payloads = listOf(Payload("OPCODE", 0, 0, PayloadType.HEX, value = "0x01"))
            ),
            response =
            ResponseSchema(
                route = FakeRoute,
                frames = listOf(
                    FrameSchema(payloads = listOf(Payload("STATUS", 0, 0, PayloadType.INTEGER)))
                )
            )
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `sends the encoded frame and decodes the observed response`() = runTest {
        val transport = FakeTransport()
        val engine = ProtocolEngine(transport)
        val results = mutableListOf<Outcome<DecodedFrame, ProtocolError>>()

        val job = launch { engine.execute(pingCommand).collect { results += it } }
        advanceUntilIdle()
        transport.incoming.emit(byteArrayOf(42))
        advanceUntilIdle()
        job.cancel()

        assertEquals(1, transport.sentFrames.size)
        assertEquals(1, transport.sentFrames[0][0])
        val success = results.single() as Outcome.Success
        assertEquals(42, success.value.values["STATUS"])
    }

    @Test
    fun `propagates a transport send failure as a protocol error`() = runTest {
        val transport =
            FakeTransport(
                sendResult = Outcome.Failure(TransportError.NotConnected("not connected"))
            )
        val engine = ProtocolEngine(transport)

        val results = mutableListOf<Outcome<DecodedFrame, ProtocolError>>()
        engine.execute(pingCommand.copy(response = null)).collect { results += it }

        assertTrue(results.single() is Outcome.Failure)
    }
}
