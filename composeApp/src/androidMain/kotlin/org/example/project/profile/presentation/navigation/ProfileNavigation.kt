package org.example.project.profile.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith

import org.example.project.core.navigation.Route
import org.example.project.profile.presentation.screens.EditProfileScreen
import org.example.project.profile.presentation.screens.ProfileScreen

@Composable
fun ProfileNavigation(
    onCreatePost:() -> Unit,
    onNavigateToPost: (String) -> Unit,
    onBack: () -> Unit
) {
    val profileBackStack = rememberNavBackStack(Route.Profile.ProfileDetail)

    NavDisplay(
        backStack = profileBackStack,
        onBack = { 
            if (profileBackStack.size > 1) {
                profileBackStack.removeLastOrNull()
            } else {
                onBack()
            }
        },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        
        transitionSpec = {
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(300)
            ) togetherWith slideOutHorizontally(
                targetOffsetX = { -it },
                animationSpec = tween(300)
            )
        },
        popTransitionSpec = {
            slideInHorizontally(
                initialOffsetX = { -it },
                animationSpec = tween(300)
            ) togetherWith slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(300)
            )
        },
        entryProvider = entryProvider {
            entry<Route.Profile.ProfileDetail>{
                ProfileScreen(
                    onNavigateToCreatePost = onCreatePost,
                    onNavigateToEditProfile = {
                        profileBackStack.add(Route.Profile.EditProfileRoute)
                    },
                    onNavigateToPost = onNavigateToPost
                )
            }
            entry<Route.Profile.EditProfileRoute>{
                EditProfileScreen(
                    onNavigateBack = {
                        if (profileBackStack.size > 1) {
                            profileBackStack.removeLastOrNull()
                        } else {
                            onBack()
                        }
                    }
                )
            }
        }
    )
}
