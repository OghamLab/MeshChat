
package com.oghamlab.meshchat.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun ContactsScreen() {
    var publicKey by remember {
        mutableStateOf("")
    }

    var status by remember {
        mutableStateOf("")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Contacts",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = publicKey,
            onValueChange = { publicKey = it },
            label = { Text("Nostr public key (npub)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = {
                status = "Contact management arrives later."
            },
            enabled = publicKey.isNotBlank()
        ) {
            Text("Add Contact")
        }

        if (status.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(status)
        }
    }
}


@Preview(showBackground = true)
@Composable
private fun ContactsScreenPreview() {
    MaterialTheme {
        ContactsScreen()
    }
}

