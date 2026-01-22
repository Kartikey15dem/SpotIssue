package org.example.project.createPost.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import org.example.project.core.navigation.Route
import org.example.project.createPost.presentation.screens.CreatePostScreen

@Composable
fun CreatePostNavigation(
    modifier : Modifier = Modifier,

) {
    val authBackStack = rememberNavBackStack(Route.CreatePost)

    NavDisplay(
        backStack = authBackStack,
        modifier = modifier,
        onBack = { authBackStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<Route.CreatePost>{
                CreatePostScreen(
                    onNavigateBack = {
                        authBackStack.removeLastOrNull()
                    }
                )
            }
        }
    )
}