package com.oghamlab.meshchat.ui.components

import android.net.http.SslCertificate.restoreState
import android.net.http.SslCertificate.saveState
import androidx.compose.material3.MaterialTheme

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.oghamlab.meshchat.navigation.Screen
import com.oghamlab.meshchat.ui.screens.OnboardingScreen

@Composable
fun AppBottomBar(navController: NavHostController) {
    val destinations = listOf(
        Screen.Conversations to "Chats",
        Screen.Contacts to "Contacts",
        Screen.Relays to "Relays",
        Screen.Settings to "Settings"
    )

    val backStackEntry =
        navController.currentBackStackEntryAsState().value

    val currentRoute = backStackEntry?.destination?.route

    NavigationBar {
        destinations.forEach { (screen, label) ->
            NavigationBarItem(
                selected = currentRoute == screen.route,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(Screen.Conversations.route) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Text(label.take(1)) },
                label = {
                    Text(
                        label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AppBottomBarPreview() {
    MaterialTheme {
        AppBottomBar(
            navController = NavHostController(LocalContext.current)
        )
    }
}

