
package com.oghamlab.meshchat.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.oghamlab.meshchat.nostr.identity.IdentityRepository

@Composable
fun RecoveryBackupScreen(
    repository: IdentityRepository
) {
    val context = LocalContext.current

    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var pendingBackup by remember {
        mutableStateOf<ByteArray?>(null)
    }

    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(
            "application/octet-stream"
        )
    ) { uri ->
        val bytes = pendingBackup
        pendingBackup = null

        if (uri != null && bytes != null) {
            try {
                context.contentResolver
                    .openOutputStream(uri)?.use {
                        it.write(bytes)
                    } ?: error("Unable to open backup destination")

                status = "Encrypted backup saved."
            } catch (e: Exception) {
                status = "Backup could not be saved."
            } finally {
                bytes.fill(0)
            }
        } else {
            bytes?.fill(0)
            status = "Backup save cancelled."
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text(
            "Encrypted Recovery Backup",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(12.dp))

        Text(
            "Choose a strong, unique recovery password. " +
                "You will need both the backup file and this " +
                "password to restore your Nostr identity."
        )

        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Recovery password") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm recovery password") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(24.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = password.length >= 12 &&
                password == confirmPassword,
            onClick = {
                val chars = password.toCharArray()

                try {
                    val backup =
                        repository.createEncryptedBackup(chars)

                    pendingBackup?.fill(0)
                    pendingBackup = backup

                    password = ""
                    confirmPassword = ""

                    saveLauncher.launch(
                        "meshchat-identity.mcbak"
                    )
                } catch (e: Exception) {
                    status = "Unable to create encrypted backup."
                } finally {
                    chars.fill('\u0000')
                }
            }
        ) {
            Text("Save Encrypted Backup")
        }

        if (status.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text(status)
        }
    }
}
