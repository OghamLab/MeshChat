
package com.oghamlab.meshchat.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

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

@Preview(showBackground = true)
@Composable
fun RelaysScreenPreview() {
    MaterialTheme {
        RelaysScreen()
    }
}