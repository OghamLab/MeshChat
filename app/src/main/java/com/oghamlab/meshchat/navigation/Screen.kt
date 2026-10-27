package com.oghamlab.meshchat.navigation



sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Conversations : Screen("conversations")
    data object Chat : Screen("chat")
    data object Contacts : Screen("contacts")
    data object Relays : Screen("relays")

    data object Identity : Screen("identity")
    data object RecoveryBackup : Screen("recovery_backup")
    data object RestoreIdentity : Screen("restore_identity")


    data object Settings : Screen("settings")
}
