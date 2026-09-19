package com.telen.protocols.core.engine

import com.telen.protocols.core.codec.DecodedFrame
import com.telen.protocols.core.codec.FrameCodec
import com.telen.protocols.core.error.Outcome
import com.telen.protocols.core.error.ProtocolError
import com.telen.protocols.core.error.TransportError
import com.telen.protocols.core.model.ProtocolCommand
import com.telen.protocols.core.model.ResponseSchema
import com.telen.protocols.core.model.RouteConfig
import com.telen.protocols.core.transport.Transport
import kotlin.time.Duration
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.timeout
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Orchestrates encode -> [Transport.send] -> [Transport.observe] -> decode using only the
 * [Transport] contract — no per-transport knowledge lives here, which is what lets a new transport
 * module (BLE, socket, future APDU) plug in without touching this class.
 */
class ProtocolEngine(private val transport: Transport) {
    fun execute(
        command: ProtocolCommand,
        values: Map<String, Any?> = emptyMap(),
        routeOverride: RouteConfig? = null
    ): Flow<Outcome<DecodedFrame, ProtocolError>> = flow {
        val request = command.request ?: return@flow
        val requestRoute = routeOverride ?: request.route

        val encoded = FrameCodec.encode(request, values)
        if (encoded is Outcome.Failure) {
            emit(Outcome.Failure(encoded.error))
            return@flow
        }
        val frame = (encoded as Outcome.Success).value

        val response = command.response
        val decodedFrames =
            response?.let { resp ->
                transport
                    .observe(routeOverride ?: resp.route)
                    .map { raw -> FrameCodec.decode(resp, raw) }
            }

        val sendResult =
            if (request.timeout > Duration.ZERO) {
                withTimeoutOrNull(request.timeout) { transport.send(requestRoute, frame) }
                    ?: Outcome.Failure(
                        TransportError.Timeout(request.timeout.inWholeMilliseconds)
                    )
            } else {
                transport.send(requestRoute, frame)
            }

        if (sendResult is Outcome.Failure) {
            emit(Outcome.Failure(ProtocolError.Transport(sendResult.error)))
            return@flow
        }

        if (response == null || decodedFrames == null) return@flow

        emitAll(
            if (response.timeout >
                Duration.ZERO
            ) {
                decodedFrames.withResponseTimeout(response)
            } else {
                decodedFrames
            }
        )
    }
}

@OptIn(FlowPreview::class)
private fun Flow<Outcome<DecodedFrame, ProtocolError>>.withResponseTimeout(
    response: ResponseSchema
): Flow<Outcome<DecodedFrame, ProtocolError>> = timeout(response.timeout).catch { cause ->
    if (cause !is TimeoutCancellationException) throw cause
    if (!response.completeOnTimeout) {
        emit(
            Outcome.Failure(
                ProtocolError.Transport(
                    TransportError.Timeout(response.timeout.inWholeMilliseconds)
                )
            )
        )
    }
}
