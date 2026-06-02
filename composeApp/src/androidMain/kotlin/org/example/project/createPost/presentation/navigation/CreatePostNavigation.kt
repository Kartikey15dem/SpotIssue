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
    onBack: () -> Unit
) {
    val createPostBackStack = rememberNavBackStack(Route.CreatePost)

    NavDisplay(
        backStack = createPostBackStack,
        modifier = modifier,
        onBack = { 
            if (createPostBackStack.size > 1) {
                createPostBackStack.removeLastOrNull()
            } else {
                onBack()
            }
        },
        entryProvider = entryProvider {
            entry<Route.CreatePost>{
                CreatePostScreen(
                    onNavigateBack = {
                        if (createPostBackStack.size > 1) {
                            createPostBackStack.removeLastOrNull()
                        } else {
                            onBack()
                        }
                    }
                )
            }
        }
    )
}
