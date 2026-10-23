
package com.oghamlab.meshchat.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

import com.oghamlab.meshchat.ui.components.AppBottomBar
import com.oghamlab.meshchat.ui.screens.*

@Composable
fun MeshChatNavGraph() {
    val navController = rememberNavController()

    val backStackEntry =
        navController.currentBackStackEntryAsState().value

    val currentRoute = backStackEntry?.destination?.route

    val showBottomBar = currentRoute in listOf(
        Screen.Conversations.route,
        Screen.Contacts.route,
        Screen.Relays.route,
        Screen.Settings.route
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                AppBottomBar(navController)
            }
        }
    ) { innerPadding ->

        NavHost(
            navController = navController,
            startDestination = Screen.Onboarding.route,
            modifier = Modifier.padding(innerPadding)
        ) {

            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onContinue = {
                        navController.navigate(
                            Screen.Conversations.route
                        ) {
                            popUpTo(Screen.Onboarding.route) {
                                inclusive = true
                            }
                        }
                    }
                )
            }

            composable(Screen.Conversations.route) {
                ConversationsScreen(
                    onOpenChat = {
                        navController.navigate(
                            Screen.Chat.route
                        )
                    }
                )
            }

            composable(Screen.Chat.route) {
                ChatScreen()
            }

            composable(Screen.Contacts.route) {
                ContactsScreen()
            }

            composable(Screen.Relays.route) {
                RelaysScreen()
            }

            composable(Screen.Settings.route) {
                SettingsScreen()
            }
        }
    }
}
