
package com.oghamlab.meshchat.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

import com.oghamlab.meshchat.nostr.identity.IdentityRepository
import com.oghamlab.meshchat.ui.components.NostrQrCode

@Composable
fun IdentityScreen(
    repository: IdentityRepository,
    onBackupClick: () -> Unit
) {
    val context = LocalContext.current
    val identity = repository.getIdentity()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "My Nostr Identity",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(24.dp))

        if (identity == null) {
            Text("No identity configured")
            return@Column
        }

        NostrQrCode(
            npub = identity.npub,
            modifier = Modifier.size(250.dp)
        )

        Spacer(Modifier.height(20.dp))

        Text(
            identity.npub,
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                val clipboard = context.getSystemService(
                    Context.CLIPBOARD_SERVICE
                ) as ClipboardManager

                clipboard.setPrimaryClip(
                    ClipData.newPlainText(
                        "Nostr Public Key",
                        identity.npub
                    )
                )
            }
        ) {
            Text("Copy Public Key")
        }

        Spacer(Modifier.height(24.dp))

        HorizontalDivider()

        Spacer(Modifier.height(24.dp))

        OutlinedButton(
            onClick = onBackupClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create Encrypted Recovery Backup")
        }

        Spacer(Modifier.height(12.dp))

        Text(
            "Your private key is never included in the public QR code.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun IdentityScreenPreview() {
    MaterialTheme {
        IdentityScreen(
            repository = IdentityRepository(LocalContext.current),
            onBackupClick = {}
        )
    }
}