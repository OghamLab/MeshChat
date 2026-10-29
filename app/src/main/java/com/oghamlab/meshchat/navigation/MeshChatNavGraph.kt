
package com.oghamlab.meshchat.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

import com.oghamlab.meshchat.ui.components.AppBottomBar
import com.oghamlab.meshchat.ui.screens.*

import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oghamlab.meshchat.nostr.identity.IdentityRepository
import com.oghamlab.meshchat.ui.viewmodel.RelayViewModel


/**
 * MeshChatNavGraph()
 *
 * PURPOSE:
 *
 * This composable manages navigation between
 * the different screens in MeshChat.
 *
 * It is responsible for:
 *
 * 1. Creating the IdentityRepository.
 *
 * 2. Checking whether the user already
 *    has a stored Nostr identity.
 *
 * 3. Choosing the initial screen.
 *
 * 4. Creating the navigation controller.
 *
 * 5. Tracking the currently displayed screen.
 *
 * 6. Showing or hiding the bottom navigation bar.
 *
 * 7. Registering the routes that users
 *    can navigate between.
 *
 * 8. Handling navigation after onboarding
 *    and when opening a conversation.
 *
 * APPLICATION FLOW:
 *
 * MeshChat launches
 *        |
 *        v
 * Check stored identity
 *        |
 *        +---- Identity exists
 *        |          |
 *        |          v
 *        |     Conversations
 *        |
 *        +---- No identity
 *                   |
 *                   v
 *               Onboarding
 *                   |
 *                   v
 *            Create / Import
 *                   |
 *                   v
 *              Conversations
 *
 * @Composable:
 *
 * This annotation tells Jetpack Compose
 * that the function describes UI behavior.
 *
 * A composable function can call other
 * composable functions.
 *
 * Compose may execute this function again
 * when observed state changes.
 */
@Composable
fun MeshChatNavGraph() {

    /**
     * Get the current Android Context.
     *
     * LocalContext is a CompositionLocal.
     *
     * A CompositionLocal makes values available
     * to composables without explicitly passing
     * them through every function.
     *
     * LocalContext.current returns the Context
     * associated with the current composition.
     *
     * Context provides access to:
     *
     * - Application resources
     * - SharedPreferences
     * - Android services
     * - Application information
     *
     * Here, Context is needed to create
     * IdentityRepository.
     */
    val context = LocalContext.current


    val relayViewModel: RelayViewModel = viewModel()

    /**
     * Create and remember IdentityRepository.
     *
     * remember() stores a value across
     * recompositions of this composable.
     *
     * Without remember(), the repository
     * would be constructed whenever this
     * composable recomposes.
     *
     * remember(context) means:
     *
     * Reuse the remembered repository
     * while the context key remains equal.
     *
     * If the context key changes,
     * recalculate the repository.
     *
     * context.applicationContext:
     *
     * Returns the application-level Context.
     *
     * This is useful for objects that may
     * live longer than an Activity.
     *
     * It reduces the risk of retaining
     * an Activity reference unnecessarily.
     *
     * Kotlin syntax:
     *
     * remember(context) {
     *     IdentityRepository(...)
     * }
     *
     * The lambda's final expression becomes
     * the remembered value.
     */
    val repository = remember(context) {
        IdentityRepository(context.applicationContext)
    }


    /**
     * Determine which screen should appear
     * when the navigation graph is created.
     *
     * remember(repository) caches the result
     * while the repository reference remains
     * unchanged.
     *
     * repository.hasIdentity() checks whether
     * SecureKeyStorage contains an identity payload.
     *
     * If an identity exists:
     *
     * Start at Conversations.
     *
     * Otherwise:
     *
     * Start at Onboarding.
     *
     * Screen.Conversations.route and
     * Screen.Onboarding.route are String values
     * defined by the Screen navigation model.
     *
     * IMPORTANT:
     *
     * This is an initial routing decision.
     *
     * It does not automatically update
     * whenever an identity is created or deleted.
     *
     * After creating an identity, navigation
     * is performed explicitly using onContinue.
     *
     * Also, hasIdentity() checks for a stored
     * payload, not necessarily a decryptable one.
     */
    val startRoute = remember(repository) {

        if (repository.hasIdentity()) {

            /**
             * Existing identity:
             *
             * Skip onboarding.
             */
            Screen.Conversations.route

        } else {

            /**
             * New installation or no
             * stored identity:
             *
             * Show onboarding.
             */
            Screen.Onboarding.route
        }
    }


    /**
     * Create the NavHostController.
     *
     * rememberNavController() creates and
     * remembers a navigation controller
     * compatible with Jetpack Compose.
     *
     * NavController manages:
     *
     * - Navigation destinations
     * - Navigation back stack
     * - Forward navigation
     * - Back navigation
     * - Route transitions
     *
     * Example:
     *
     * navController.navigate("chat")
     *
     * tells the navigation system to
     * open the destination registered
     * under the route "chat".
     *
     * The controller is remembered across
     * ordinary recompositions.
     */
    val navController = rememberNavController()


    /**
     * Observe the current navigation
     * back stack entry.
     *
     * currentBackStackEntryAsState()
     * converts the currently active back stack
     * entry into observable Compose State.
     *
     * .value retrieves the current value.
     *
     * When the active navigation destination
     * changes, Compose observes the change
     * and can recompose the relevant UI.
     *
     * A NavBackStackEntry contains information
     * about a destination on the back stack.
     *
     * It can include:
     *
     * - Destination
     * - Route arguments
     * - Lifecycle information
     * - Saved state
     */
    val backStackEntry =
        navController.currentBackStackEntryAsState().value


    /**
     * Retrieve the route of the currently
     * displayed navigation destination.
     *
     * backStackEntry?.destination?.route
     *
     * The ?. operator is Kotlin's
     * safe-call operator.
     *
     * It accesses a property only if
     * the preceding object is not null.
     *
     * For example:
     *
     * If backStackEntry is null,
     * currentRoute becomes null.
     *
     * Otherwise:
     *
     * Access destination.route.
     *
     * This prevents a NullPointerException
     * when the navigation entry is not
     * available yet.
     *
     * currentRoute has type String?.
     */
    val currentRoute =
        backStackEntry?.destination?.route


    /**
     * Determine whether the bottom
     * navigation bar should be displayed.
     *
     * The "in" operator checks whether
     * an item belongs to a collection.
     *
     * listOf(...) creates a read-only List
     * containing the specified routes.
     *
     * The expression:
     *
     * currentRoute in listOf(...)
     *
     * returns true if currentRoute matches
     * one of the listed routes.
     *
     * Show the bottom bar on:
     *
     * - Conversations
     * - Contacts
     * - Relays
     * - Settings
     *
     * Hide it on:
     *
     * - Onboarding
     * - Chat
     *
     * This allows the Chat screen to
     * occupy the available screen area
     * without the main bottom navigation.
     *
     * showBottomBar is a Boolean.
     */
    val showBottomBar = currentRoute in listOf(
        Screen.Conversations.route,
        Screen.Contacts.route,
        Screen.Relays.route,
        Screen.Settings.route
    )


    /**
     * Scaffold is a Material 3 layout component.
     *
     * It provides slots for common app
     * layout elements such as:
     *
     * - Top app bar
     * - Bottom navigation bar
     * - Floating action button
     * - Snackbar host
     * - Main screen content
     *
     * Here, we use:
     *
     * bottomBar:
     *   Displays the main navigation tabs.
     *
     * content:
     *   Contains the NavHost and screens.
     */
    Scaffold(

        /**
         * bottomBar is a composable lambda.
         *
         * It defines what should appear
         * in Scaffold's bottom bar area.
         *
         * If showBottomBar is true,
         * render AppBottomBar.
         *
         * Otherwise, the lambda produces
         * no UI content.
         */
        bottomBar = {

            if (showBottomBar) {

                /**
                 * Display the application's
                 * bottom navigation component.
                 *
                 * navController is passed so
                 * AppBottomBar can navigate
                 * between the main sections.
                 */
                AppBottomBar(navController)
            }
        }

    ) { innerPadding ->

        /**
         * This trailing lambda defines
         * Scaffold's main content.
         *
         * innerPadding is supplied
         * automatically by Scaffold.
         *
         * It contains padding needed
         * to account for layout elements
         * such as the bottom bar.
         *
         * The -> symbol separates
         * lambda parameters from its body.
         *
         * Syntax:
         *
         * { parameter ->
         *     // Lambda body
         * }
         *
         * Without applying innerPadding,
         * navigation content may appear
         * underneath Scaffold's bars.
         */

        /**
         * NavHost displays the destination
         * selected by NavController.
         *
         * Parameters:
         *
         * navController:
         *   Controls navigation.
         *
         * startDestination:
         *   Route displayed initially.
         *
         * modifier:
         *   Applies layout modifications.
         *
         * Modifier.padding(innerPadding):
         *   Respects Scaffold's content insets.
         *
         * The trailing lambda registers
         * the navigation destinations.
         */
        NavHost(
            navController = navController,
            startDestination = startRoute,
            modifier = Modifier.padding(innerPadding)
        ) {

            /**
             * ONBOARDING DESTINATION
             *
             * composable(route) registers
             * a navigation destination.
             *
             * When the active route matches
             * Screen.Onboarding.route,
             * the navigation system displays
             * OnboardingScreen.
             */
            composable(Screen.Onboarding.route) {

                OnboardingScreen(

                    /**
                     * Pass the identity repository
                     * to the onboarding screen.
                     *
                     * OnboardingScreen can use it
                     * to create or import identities.
                     */
                    repository = repository,

                    /**
                     * Pass a callback to execute
                     * when onboarding is completed.
                     *
                     * onContinue is a lambda.
                     *
                     * We are defining what
                     * should happen later,
                     * rather than navigating
                     * immediately.
                     *
                     * The screen decides when
                     * to invoke onContinue().
                     */
                    onContinue = {

                        /**
                         * Navigate to Conversations
                         * after identity setup.
                         */
                        navController.navigate(
                            Screen.Conversations.route
                        ) {

                            /**
                             * popUpTo() modifies
                             * the navigation back stack.
                             *
                             * It removes destinations
                             * up to a specified route.
                             *
                             * Here, the target is
                             * Screen.Onboarding.route.
                             *
                             * inclusive = true:
                             *
                             * Also remove the
                             * Onboarding destination.
                             *
                             * This prevents the user
                             * from pressing Back and
                             * returning to onboarding
                             * after creating an identity.
                             *
                             * Example:
                             *
                             * BEFORE:
                             *
                             * [Onboarding]
                             *
                             * AFTER:
                             *
                             * [Conversations]
                             */
                            popUpTo(Screen.Onboarding.route) {
                                inclusive = true
                            }
                        }
                    },
                    onRestoreBackup = {
                        navController.navigate(
                            Screen.RestoreIdentity.route
                        )
                    }
                )





            }


            /**
             * CONVERSATIONS DESTINATION
             *
             * Displays the user's list
             * of conversations.
             *
             * Each conversation can be
             * opened to display ChatScreen.
             */
            composable(Screen.Conversations.route) {

                ConversationsScreen(

                    /**
                     * onOpenChat is a callback
                     * supplied to ConversationsScreen.
                     *
                     * The screen may call it when
                     * the user taps a conversation.
                     *
                     * This keeps navigation logic
                     * outside the screen itself.
                     */
                    onOpenChat = {

                        /**
                         * Navigate to ChatScreen.
                         *
                         * In the current version,
                         * no conversation identifier
                         * is passed to ChatScreen.
                         *
                         * A future implementation
                         * should identify which
                         * conversation was selected.
                         */
                        navController.navigate(
                            Screen.Chat.route
                        )
                    }
                )
            }


            /**
             * CHAT DESTINATION
             *
             * Displays the chat interface.
             *
             * The bottom navigation bar
             * is hidden on this route.
             *
             * Later, this screen will:
             *
             * - Display messages.
             * - Send encrypted Nostr DMs.
             * - Show message status.
             * - Support attachments.
             *
             * Currently, no arguments
             * are passed to ChatScreen.
             */
            composable(Screen.Chat.route) {
                ChatScreen()
            }


            /**
             * CONTACTS DESTINATION
             *
             * Displays saved contacts.
             *
             * A future implementation
             * could show:
             *
             * - npub identifiers
             * - Contact names
             * - Profile metadata
             * - Contact search
             */
            composable(Screen.Contacts.route) {
                ContactsScreen()
            }


            /**
             * RELAYS DESTINATION
             *
             * Displays Nostr relay settings.
             *
             * This screen may eventually
             * allow users to:
             *
             * - Add relay URLs.
             * - Remove relays.
             * - Connect/disconnect.
             * - View relay status.
             * - Configure relay preferences.
             */

            composable(Screen.Relays.route) {
                RelaysScreen(
                    viewModel = relayViewModel
                )
            }
            /**
             * SETTINGS DESTINATION
             *
             * Displays application settings.
             *
             * The repository is passed
             * to SettingsScreen.
             *
             * This allows the screen
             * to access identity operations
             * such as:
             *
             * - Viewing the user's npub.
             * - Exporting their nsec.
             *
             * Sensitive operations should
             * require appropriate safeguards.
             */


            composable(Screen.Settings.route) {
                SettingsScreen(
                    repository = repository,
                    onIdentityClick = {
                        navController.navigate(Screen.Identity.route)
                    }
                )
            }




            composable(Screen.Identity.route) {
                IdentityScreen(
                    repository = repository,
                    onBackupClick = {
                        navController.navigate(
                            Screen.RecoveryBackup.route
                        )
                    }
                )
            }






            composable(Screen.RecoveryBackup.route) {
                RecoveryBackupScreen(
                    repository = repository
                )
            }


            /**
             * RESTORE IDENTITY NAVIGATION DESTINATION
             *
             * This block belongs inside the NavHost { ... }
             * section of MeshChatNavGraph().
             *
             * PURPOSE:
             *
             * 1. Register the Restore Identity route.
             * 2. Display RestoreIdentityScreen.
             * 3. Pass IdentityRepository to the screen.
             * 4. Wait for successful identity restoration.
             * 5. Navigate to Conversations.
             * 6. Remove Onboarding from the back stack.
             *
             * EXPECTED USER FLOW:
             *
             * Onboarding
             *     |
             *     v
             * Restore Identity
             *     |
             *     v
             * Select backup and enter recovery password
             *     |
             *     v
             * Restore Nostr private key
             *     |
             *     v
             * Conversations
             */
            composable(Screen.RestoreIdentity.route) {

                /**
                 * composable() registers a navigation destination.
                 *
                 * Screen.RestoreIdentity.route:
                 *
                 * This is the route identifier for the
                 * Restore Identity screen.
                 *
                 * For example, your Screen class might contain:
                 *
                 * object RestoreIdentity : Screen("restore_identity")
                 *
                 * When NavController navigates to this route,
                 * Jetpack Navigation displays the content
                 * declared inside this lambda.
                 */

                /**
                 * Display RestoreIdentityScreen.
                 *
                 * This composable is responsible for
                 * the identity restoration interface.
                 *
                 * It might contain:
                 *
                 * - Select encrypted backup file.
                 * - Enter recovery password.
                 * - Restore button.
                 * - Loading indicator.
                 * - Error messages.
                 *
                 * The actual restoration logic should
                 * happen in a repository or ViewModel,
                 * not directly inside the navigation graph.
                 */
                RestoreIdentityScreen(

                    /**
                     * Pass IdentityRepository to the screen.
                     *
                     * repository = repository
                     *
                     * The left-hand repository is the
                     * parameter defined by RestoreIdentityScreen.
                     *
                     * The right-hand repository is the
                     * IdentityRepository instance created
                     * in MeshChatNavGraph().
                     *
                     * IdentityRepository coordinates:
                     *
                     * - Nostr private-key validation.
                     * - Identity creation and import.
                     * - Secure local key storage.
                     *
                     * IMPORTANT:
                     *
                     * The existing IdentityRepository must
                     * have a suitable method for importing
                     * the decrypted backup key.
                     *
                     * Its importIdentity(nsec) method accepts
                     * an nsec String, not a raw ByteArray.
                     */
                    repository = repository,

                    /**
                     * onRestored is a callback.
                     *
                     * It tells RestoreIdentityScreen what
                     * should happen after restoration succeeds.
                     *
                     * A callback is a function passed
                     * to another function.
                     *
                     * Kotlin syntax:
                     *
                     * onRestored = {
                     *     // Code to execute later
                     * }
                     *
                     * This lambda does not execute
                     * immediately when the screen appears.
                     *
                     * RestoreIdentityScreen should invoke
                     * onRestored() only after:
                     *
                     * 1. The encrypted backup is decrypted.
                     * 2. The recovered private key is validated.
                     * 3. The identity is securely stored.
                     * 4. The storage operation succeeds.
                     *
                     * If restoration fails, the screen
                     * should display an error instead
                     * of calling onRestored().
                     */
                    onRestored = {

                        /**
                         * Navigate to Conversations.
                         *
                         * navController is the navigation
                         * controller created earlier using:
                         *
                         * rememberNavController()
                         *
                         * navigate() changes the active
                         * destination.
                         *
                         * Screen.Conversations.route
                         * identifies the Conversations screen.
                         *
                         * The trailing lambda:
                         *
                         * { popUpTo(...) { ... } }
                         *
                         * configures the navigation operation.
                         */
                        navController.navigate(
                            Screen.Conversations.route
                        ) {

                            /**
                             * popUpTo() modifies the
                             * navigation back stack.
                             *
                             * The back stack contains the
                             * history of navigation destinations.
                             *
                             * For example:
                             *
                             * [Onboarding]
                             * [Restore Identity]
                             *
                             * Normally, navigating to
                             * Conversations would produce:
                             *
                             * [Onboarding]
                             * [Restore Identity]
                             * [Conversations]
                             *
                             * This would allow the user
                             * to press Back and return
                             * to the restoration screens.
                             *
                             * We want to prevent that
                             * after successful restoration.
                             *
                             * popUpTo(Screen.Onboarding.route)
                             *
                             * removes destinations above
                             * Onboarding from the back stack.
                             */
                            popUpTo(Screen.Onboarding.route) {

                                /**
                                 * inclusive = true
                                 *
                                 * Also removes Onboarding
                                 * itself from the back stack.
                                 *
                                 * Without inclusive = true:
                                 *
                                 * Onboarding would remain.
                                 *
                                 * With inclusive = true:
                                 *
                                 * Onboarding and the
                                 * Restore Identity screen
                                 * are removed.
                                 *
                                 * EXPECTED BACK STACK:
                                 *
                                 * BEFORE:
                                 *
                                 * [Onboarding]
                                 * [Restore Identity]
                                 *
                                 * AFTER:
                                 *
                                 * [Conversations]
                                 *
                                 * Therefore, pressing Back
                                 * will not return the user
                                 * to onboarding.
                                 *
                                 * NOTE:
                                 *
                                 * This assumes Onboarding
                                 * exists in the current
                                 * navigation back stack.
                                 */
                                inclusive = true
                            }
                        }
                    }
                )
            }





        }
    }
}
