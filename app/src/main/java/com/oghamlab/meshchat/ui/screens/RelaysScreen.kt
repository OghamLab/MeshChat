
package com.oghamlab.meshchat.ui.screens
/*

@Composable
fun RelaysScreen() {
    var relayUrl by remember {
        mutableStateOf("")
    }

    val relays = remember {
        mutableStateListOf<String>()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Nostr Relays",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(8.dp))

        Text("Saved in memory only. No active connections.")

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = relayUrl,
            onValueChange = { relayUrl = it },
            label = { Text("Relay URL") },
            placeholder = { Text("wss://relay.example") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = {
                val url = relayUrl.trim()

                if (url.startsWith("wss://") &&
                    url.length > "wss://".length &&
                    url !in relays
                ) {
                    relays.add(url)
                    relayUrl = ""
                }
            }
        ) {
            Text("Add Relay")
        }

        Spacer(Modifier.height(16.dp))

        relays.forEach { relay ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Text(
                    relay,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

*/




import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oghamlab.meshchat.nostr.identity.relay.RelayInfo
import com.oghamlab.meshchat.nostr.identity.relay.RelayStatus
import com.oghamlab.meshchat.ui.viewmodel.RelayViewModel

// Stateful composable entry point for production app navigation.
@Composable
fun RelaysScreen(
    viewModel: RelayViewModel = viewModel()
) {
    val relays by viewModel.relays.collectAsStateWithLifecycle()
    val eventCount by viewModel.eventCount.collectAsStateWithLifecycle()

    RelaysScreenContent(
        relays = relays,
        eventCount = eventCount,
        onAddRelay = viewModel::addRelay,
        onSubscribePublicEvents = viewModel::subscribePublicEvents,
        onReconnectAll = viewModel::reconnectAll,
        onRemoveRelay = viewModel::removeRelay
    )
}

// Stateless UI composable separated to allow previewing without instantiating ViewModel/network classes.
@Composable
fun RelaysScreenContent(
    relays: List<RelayInfo>,
    eventCount: Int,
    onAddRelay: (String) -> Boolean,
    onSubscribePublicEvents: () -> Unit,
    onReconnectAll: () -> Unit,
    onRemoveRelay: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var relayUrl by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Nostr Relays",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(8.dp))

        Text("Unique event IDs observed: $eventCount")
        Text(
            "Public test events only; not signature-verified",
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = relayUrl,
            onValueChange = {
                relayUrl = it
                error = ""
            },
            label = { Text("Relay URL") },
            placeholder = { Text("wss://relay.example") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                if (onAddRelay(relayUrl)) {
                    relayUrl = ""
                } else {
                    error = "Enter a valid wss:// relay URL."
                }
            },
            enabled = relayUrl.isNotBlank()
        ) {
            Text("Add Relay")
        }

        if (error.isNotBlank()) {
            Text(
                error,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onSubscribePublicEvents
            ) {
                Text("Test Subscribe")
            }

            OutlinedButton(
                onClick = onReconnectAll
            ) {
                Text("Reconnect")
            }
        }

        Spacer(Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(relays, key = { it.url }) { relay ->
                RelayCard(
                    relay = relay,
                    onRemove = {
                        onRemoveRelay(relay.url)
                    }
                )
            }
        }
    }
}

@Composable
private fun RelayCard(
    relay: RelayInfo,
    onRemove: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                relay.url,
                style = MaterialTheme.typography.titleSmall
            )

            Spacer(Modifier.height(8.dp))

            val statusText = when (relay.status) {
                RelayStatus.CONNECTED -> "Connected"
                RelayStatus.CONNECTING -> "Connecting"
                RelayStatus.RECONNECTING -> "Reconnecting"
                RelayStatus.DISCONNECTED -> "Disconnected"
                RelayStatus.ERROR -> "Error"
            }

            Text("Status: $statusText")
            Text("Events observed: ${relay.receivedEvents}")

            if (relay.historyComplete) {
                Text("Initial event history complete")
            }

            relay.lastError?.let {
                Text(
                    "Relay message: $it",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(Modifier.height(8.dp))

            TextButton(onClick = onRemove) {
                Text("Remove")
            }
        }
    }
}

// Preview uses the stateless RelaysScreenContent with sample data to avoid initializing OkHttpClient/network dependencies during Compose preview rendering in Layoutlib.
@Preview(showBackground = true)
@Composable
fun RelaysScreenPreview() {
    MaterialTheme {
        RelaysScreenContent(
            relays = listOf(
                RelayInfo(
                    url = "wss://relay.damus.io",
                    status = RelayStatus.CONNECTED,
                    receivedEvents = 42,
                    historyComplete = true
                ),
                RelayInfo(
                    url = "wss://nos.lol",
                    status = RelayStatus.DISCONNECTED,
                    receivedEvents = 0,
                    historyComplete = false
                )
            ),
            eventCount = 42,
            onAddRelay = { true },
            onSubscribePublicEvents = {},
            onReconnectAll = {},
            onRemoveRelay = {}
        )
    }
}
