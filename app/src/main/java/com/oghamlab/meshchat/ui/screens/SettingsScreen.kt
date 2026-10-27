
package com.oghamlab.meshchat.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp




import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.oghamlab.meshchat.nostr.identity.IdentityRepository

@Composable
fun SettingsScreen(
    repository: IdentityRepository,
    onIdentityClick: () -> Unit
) {
    val context = LocalContext.current

    var identity by remember {
        mutableStateOf(repository.getIdentity())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Settings",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(20.dp))

        Text(
            "Nostr Identity",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(Modifier.height(12.dp))

        if (identity != null) {
            Text(
                "Your Public Key (npub)",
                style = MaterialTheme.typography.titleSmall
            )

            Spacer(Modifier.height(8.dp))

            Text(
                identity!!.npub,
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = {
                    val clipboard = context.getSystemService(
                        Context.CLIPBOARD_SERVICE
                    ) as ClipboardManager

                    clipboard.setPrimaryClip(
                        ClipData.newPlainText(
                            "Nostr Public Key",
                            identity!!.npub
                        )
                    )
                }
            ) {
                Text("Copy Public Key")
            }
        } else {
            Text("No identity configured.")
        }

        Spacer(Modifier.height(24.dp))

        HorizontalDivider()

        Spacer(Modifier.height(16.dp))

        Text("Private key: Encrypted on device")

        Spacer(Modifier.height(8.dp))

        Text("Nostr relay connection: Not configured")

        Spacer(Modifier.height(8.dp))

        Text("Encrypted messaging: Coming later")

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = onIdentityClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("My Identity & Recovery")
        }



    }
}








@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    MaterialTheme {
        SettingsScreen(
            repository = IdentityRepository(LocalContext.current),
            onIdentityClick = {}

        )
    }
}
