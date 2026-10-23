package com.oghamlab.meshchat.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

data class ConversationPreview(
    val name: String,
    val lastMessage: String
)

@Composable
fun ConversationsScreen(
    onOpenChat: () -> Unit
) {
    val conversations = listOf(
        ConversationPreview("Alice", "Hello!"),
        ConversationPreview("Bob", "How are you?"),
        ConversationPreview("Charlie", "Talk later.")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Conversations",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(conversations) { conversation ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenChat() }
//also could be done like this .clickable(onClick = onOpenChat)

                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            conversation.name,
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            conversation.lastMessage,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}
@Preview(showBackground = true)
@Composable
fun ConversationsScreenPreview() {
    MaterialTheme {
        ConversationsScreen(
            onOpenChat = {}
        )
    }
}