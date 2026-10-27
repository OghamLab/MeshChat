
package com.oghamlab.meshchat.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.oghamlab.meshchat.nostr.identity.IdentityRepository

@Composable
fun RestoreIdentityScreen(
    repository: IdentityRepository,
    onRestored: () -> Unit
) {
    val context = LocalContext.current

    var selectedBackup by remember {
        mutableStateOf<ByteArray?>(null)
    }
    var password by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        selectedBackup?.fill(0)
        selectedBackup = null

        if (uri != null) {
            try {

                val bytes = context.contentResolver
                    .openInputStream(uri)?.use { stream ->
                        val output = java.io.ByteArrayOutputStream()
                        val buffer = ByteArray(82)

                        while (output.size() < 82) {
                            val count = stream.read(
                                buffer,
                                0,
                                82 - output.size()
                            )
                            if (count == -1) break
                            output.write(buffer, 0, count)
                        }

                        output.toByteArray()
                    } ?: error("Unable to read backup")



            } catch (e: Exception) {
                status = "Invalid or unreadable backup file."
            }



        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text(
            "Restore Nostr Identity",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(20.dp))

        OutlinedButton(
            onClick = {
                picker.launch(
                    arrayOf("application/octet-stream", "*/*")
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Choose Encrypted Backup")
        }

        Spacer(Modifier.height(20.dp))

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

        Spacer(Modifier.height(20.dp))

        Button(
            enabled = selectedBackup != null &&
                password.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val chars = password.toCharArray()

                try {
                    repository.restoreEncryptedBackup(
                        selectedBackup!!,
                        chars
                    )

                    password = ""
                    selectedBackup?.fill(0)
                    selectedBackup = null

                    onRestored()
                } catch (e: Exception) {
                    status = "Unable to restore identity. " +
                        "Check the password and backup file."
                } finally {
                    chars.fill('\u0000')
                }
            }
        ) {
            Text("Restore Identity")
        }

        if (status.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text(status)
        }
    }
}
