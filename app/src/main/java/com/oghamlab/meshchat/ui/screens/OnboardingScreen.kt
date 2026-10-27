package com.oghamlab.meshchat.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.oghamlab.meshchat.nostr.identity.IdentityRepository

@Composable
fun OnboardingScreen(
    repository: IdentityRepository,
    onContinue: () -> Unit,
    onRestoreBackup: () -> Unit
) {
    var nsec by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Welcome to MeshChat",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(12.dp))

        Text("Create or import your Nostr identity.")

        Spacer(Modifier.height(24.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = !busy,
            onClick = {
                busy = true
                try {
                    repository.createIdentity()
                    onContinue()
                } catch (e: Exception) {
                    error = "Unable to create identity."
                } finally {
                    busy = false
                }
            }
        ) {
            Text("Create New Identity")
        }

        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = nsec,
            onValueChange = {
                nsec = it
                error = null
            },
            label = { Text("Existing nsec") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            enabled = !busy && nsec.isNotBlank(),
            onClick = {
                busy = true
                try {
                    repository.importIdentity(nsec)
                    nsec = ""
                    onContinue()
                } catch (e: IllegalArgumentException) {
                    error = "Invalid Nostr private key."
                } catch (e: Exception) {
                    error = "Unable to import identity."
                } finally {
                    busy = false
                }
            }
        ) {
            Text("Import Identity")
        }

        error?.let {
            Spacer(Modifier.height(12.dp))
            Text(
                it,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(Modifier.height(20.dp))


        OutlinedButton(
            onClick = onRestoreBackup,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Restore Encrypted Backup")
        }

        Spacer(Modifier.height(20.dp))





        Text(
            "Keep your private key secret. Losing it without " +
                    "a backup can permanently lock you out of your identity.",
            style = MaterialTheme.typography.bodySmall
        )
    }





}












@Preview(showBackground = true)
@Composable
fun OnboardingScreenPreview() {
    MaterialTheme {
        OnboardingScreen(
            repository = IdentityRepository(LocalContext.current),
            onContinue = {},
            onRestoreBackup = {}
        )
    }
}
