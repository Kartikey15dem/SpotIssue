package org.example.project.createPost.presentation.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import org.example.project.core.navigation.Route
import org.example.project.createPost.presentation.screens.CreatePostScreen

@Composable
fun CreatePostNavigation(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
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
        entryDecorators =
            listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
        transitionSpec = {
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(300),
            ) togetherWith
                slideOutHorizontally(
                    targetOffsetX = { -it },
                    animationSpec = tween(300),
                )
        },
        popTransitionSpec = {
            slideInHorizontally(
                initialOffsetX = { -it },
                animationSpec = tween(300),
            ) togetherWith
                slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(300),
                )
        },
        entryProvider =
            entryProvider {
                entry<Route.CreatePost> {
                    CreatePostScreen(
                        onNavigateBack = {
                            if (createPostBackStack.size > 1) {
                                createPostBackStack.removeLastOrNull()
                            } else {
                                onBack()
                            }
                        },
                    )
                }
            },
    )
}
