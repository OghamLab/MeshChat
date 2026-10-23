package com.oghamlab.meshchat.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun ChatScreen() {
    var message by rememberSaveable {
        mutableStateOf("")
    }

    val messages = remember {
        mutableStateListOf(
            "Alice: Hello!",
            "Me: Hi Alice!"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Demo Conversation",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            "Local prototype only — not encrypted or transmitted",
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { item ->
                Card(Modifier.fillMaxWidth()) {
                    Text(
                        text = item,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                placeholder = { Text("Message") },
                modifier = Modifier.weight(1f),
                maxLines = 4
            )

            Button(
                enabled = message.isNotBlank(),
                onClick = {
                    val text = message.trim()
                    if (text.isNotEmpty()) {
                        messages.add("Me: $text")
                        message = ""
                    }
                }
            ) {
                Text("Send")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ChatScreenPreview() {
    MaterialTheme {
        ChatScreen()
    }
}
