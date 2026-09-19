package com.telen.protocols.sample

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.telen.protocols.core.engine.ProtocolEngine
import com.telen.protocols.core.error.Outcome
import com.telen.protocols.core.model.DeviceProtocol
import com.telen.protocols.transport.ble.AndroidGattApi
import com.telen.protocols.transport.ble.BleTarget
import com.telen.protocols.transport.ble.BleTransport
import com.telen.protocols.transport.socket.SocketTransport
import com.telen.protocols.transport.socket.TcpRoute
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch

/**
 * The Phase 6 milestone: both real devices (led_ribbon.json over socket, minger.json over BLE)
 * driven through nothing but the public protocol-core API — no direct dependence on RouteConfig
 * subtypes beyond overriding the LED ribbon's address, exactly as the engine's `routeOverride`
 * parameter is meant to be used.
 */
@Composable
fun DemoScreen(context: Context) {
    val scope = rememberCoroutineScope()

    val ledProtocol = remember { loadLedRibbonProtocol(context) }
    val ledEngine = remember { ProtocolEngine(SocketTransport()) }
    var ledAddress by remember { mutableStateOf("") }
    var ledStatus by remember { mutableStateOf("Enter the LED ribbon's IP address") }

    val mingerProtocol = remember { loadMingerProtocol(context) }
    val bleTransport = remember { BleTransport(AndroidGattApi(context)) }
    val mingerEngine = remember { ProtocolEngine(bleTransport) }
    var mingerMac by remember { mutableStateOf("") }
    var mingerStatus by remember { mutableStateOf("Enter the Minger P50's MAC address") }

    Column(
        modifier =
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("HexProtocols sample", style = MaterialTheme.typography.headlineSmall)

        Text("LED ribbon (socket)", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = ledAddress,
            onValueChange = { ledAddress = it },
            label = { Text("IP address") },
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                scope.launch {
                    ledStatus =
                        runLightCommand(ledEngine, ledProtocol, ledAddress, on = true)
                }
            }) { Text("Light on") }
            Button(onClick = {
                scope.launch {
                    ledStatus =
                        runLightCommand(ledEngine, ledProtocol, ledAddress, on = false)
                }
            }) { Text("Light off") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                scope.launch {
                    ledStatus =
                        runLedChangeColor(ledEngine, ledProtocol, ledAddress, 255, 0, 0)
                }
            }) { Text("Red") }
            Button(onClick = {
                scope.launch {
                    ledStatus =
                        runLedChangeColor(ledEngine, ledProtocol, ledAddress, 0, 255, 0)
                }
            }) { Text("Green") }
            Button(onClick = {
                scope.launch {
                    ledStatus =
                        runLedChangeColor(ledEngine, ledProtocol, ledAddress, 0, 0, 255)
                }
            }) { Text("Blue") }
        }
        Text(ledStatus, style = MaterialTheme.typography.bodySmall)

        HorizontalDivider()

        Text("Minger P50 (BLE)", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = mingerMac,
            onValueChange = { mingerMac = it },
            label = { Text("MAC address") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = {
            scope.launch { mingerStatus = connectMinger(bleTransport, mingerMac) }
        }) { Text("Connect") }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                scope.launch {
                    mingerStatus =
                        runMingerChangeColor(mingerEngine, mingerProtocol, 255, 0, 0)
                }
            }) { Text("Red") }
            Button(onClick = {
                scope.launch {
                    mingerStatus =
                        runMingerChangeColor(mingerEngine, mingerProtocol, 0, 255, 0)
                }
            }) { Text("Green") }
            Button(onClick = {
                scope.launch {
                    mingerStatus =
                        runMingerChangeColor(mingerEngine, mingerProtocol, 0, 0, 255)
                }
            }) { Text("Blue") }
        }
        Text(mingerStatus, style = MaterialTheme.typography.bodySmall)
    }
}

private suspend fun runLightCommand(
    engine: ProtocolEngine,
    protocol: DeviceProtocol,
    address: String,
    on: Boolean
): String {
    val command =
        protocol.command(if (on) "LIGHT_ON" else "LIGHT_OFF") ?: return "Command not found"
    val route =
        (command.request?.route as? TcpRoute)?.copy(address = address) ?: return "No TCP route"
    val checksum = if (on) {
        ledRibbonChecksum(
            0x71,
            0x23,
            0x0F
        )
    } else {
        ledRibbonChecksum(0x71, 0x24, 0x0F)
    }
    return describe(
        engine.execute(
            command,
            values = mapOf("CHECKSUM" to checksum),
            routeOverride = route
        ).toList()
    )
}

private suspend fun runLedChangeColor(
    engine: ProtocolEngine,
    protocol: DeviceProtocol,
    address: String,
    red: Int,
    green: Int,
    blue: Int
): String {
    val command = protocol.command("CHANGE_COLOR") ?: return "Command not found"
    val route =
        (command.request?.route as? TcpRoute)?.copy(address = address) ?: return "No TCP route"
    val checksum = ledRibbonChecksum(0x31, red, green, blue, 0, 0, 0x0F)
    val values = mapOf("RED" to red, "GREEN" to green, "BLUE" to blue, "CHECKSUM" to checksum)
    return describe(engine.execute(command, values = values, routeOverride = route).toList())
}

private suspend fun connectMinger(transport: BleTransport, macAddress: String): String {
    val result = transport.connect(BleTarget(macAddress = macAddress))
    return when (result) {
        is Outcome.Success -> "Connected"
        is Outcome.Failure -> "Connection failed: ${result.error}"
    }
}

private suspend fun runMingerChangeColor(
    engine: ProtocolEngine,
    protocol: DeviceProtocol,
    red: Int,
    green: Int,
    blue: Int
): String {
    val command = protocol.command("CHANGE_COLOR") ?: return "Command not found"
    val values = mapOf("RED" to red, "GREEN" to green, "BLUE" to blue)
    return describe(engine.execute(command, values = values).toList())
}

private fun describe(results: List<Outcome<*, *>>): String {
    val failure = results.filterIsInstance<Outcome.Failure<*>>().firstOrNull()
    return if (failure != null) "Failed: ${failure.error}" else "OK"
}
