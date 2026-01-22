package org.example.project.profile.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import org.example.project.core.navigation.Route
import org.example.project.profile.presentation.screens.EditProfileScreen
import org.example.project.profile.presentation.screens.ProfileScreen

@Composable
fun ProfileNavigation(
    modifier : Modifier = Modifier,
    onCreatePost:() -> Unit
) {
    val profileBackStack = rememberNavBackStack(Route.Profile.ProfileDetail)

    NavDisplay(
        backStack = profileBackStack,
        onBack = { profileBackStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<Route.Profile.ProfileDetail>{
                ProfileScreen(
                    onNavigateToCreatePost = onCreatePost,
                    onNavigateToEditProfile = {
                        profileBackStack.add(Route.Profile.EditProfileRoute)
                    }
                )
            }
            entry<Route.Profile.EditProfileRoute>{
                EditProfileScreen(
                    onNavigateBack = {}
                )


            }


        }
    )
}