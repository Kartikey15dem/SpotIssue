package org.example.project.core.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import org.example.project.auth.presentation.navigation.AuthNavigation
import org.example.project.auth.presentation.screens.LocationFetchScreenWithPermissions
import org.example.project.createPost.presentation.navigation.CreatePostNavigation
import org.example.project.home.presentation.navigation.HomeNavigation
import org.example.project.profile.presentation.navigation.ProfileNavigation

@Composable
fun NavigationRoot(start: NavKey) {
    val rootBackStack = rememberNavBackStack(start)

//    composable(
//        route = "post_details/{postId}",
//        arguments = listOf(navArgument("postId") { type = NavType.StringType }),
//
//
//        deepLinks = listOf(
//            navDeepLink {
//                uriPattern = "https://www.issuespot.com/post/{postId}"
//                action = Intent.ACTION_VIEW
//            }
//        )
//    ) { backStackEntry ->
//        val postId = backStackEntry.arguments?.getString("postId")
//        // Navigate to your specific Post Details screen
//        PostDetailScreen(postId = postId)
//    }

    LaunchedEffect(start) {
        val currentFirst = rootBackStack.firstOrNull()
        if (currentFirst == null) {
            rootBackStack.add(start)
            return@LaunchedEffect
        }

        val isCurrentAuth = currentFirst is Route.Auth || currentFirst == Route.Auth
        val isStartAuth = start is Route.Auth || start == Route.Auth

        if (isCurrentAuth != isStartAuth) {
            rootBackStack.clear()
            rootBackStack.add(start)
        }
    }
    NavDisplay(
        backStack = rootBackStack,
        onBack = { rootBackStack.removeLastOrNull() },
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
                entry<Route.Auth> {
                    AuthNavigation(
                        onBack = {
                            rootBackStack.removeLastOrNull()
                        },
                    )
                }
                entry<Route.LocationFetch> {
                    LocationFetchScreenWithPermissions(
                        onLocationFetched = {
                            rootBackStack.clear()
                            rootBackStack.add(Route.Home)
                        },
                    )
                }
                entry<Route.Home> {
                    HomeNavigation(
                        onCreatePost = {
                            rootBackStack.add(Route.CreatePost)
                        },
                        onProfileClick = {
                            rootBackStack.add(Route.Profile)
                        },
                        onBack = {
                            rootBackStack.removeLastOrNull()
                        },
                    )
                }
                entry<Route.Profile> {
                    ProfileNavigation(
                        onCreatePost = {
                            rootBackStack.add(Route.CreatePost)
                        },
                        onNavigateToPost = { postId ->
                            rootBackStack.add(Route.PostDetail(postId))
                        },
                        onBack = {
                            rootBackStack.removeLastOrNull()
                        },
                    )
                }
                entry<Route.CreatePost> {
                    CreatePostNavigation(
                        onBack = {
                            rootBackStack.removeLastOrNull()
                        },
                    )
                }
            },
        modifier =
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars),
    )
}
