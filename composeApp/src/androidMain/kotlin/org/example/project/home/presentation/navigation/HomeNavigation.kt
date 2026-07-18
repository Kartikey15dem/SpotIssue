package org.example.project.home.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import org.example.project.feature.home.CurrentLevelManager
import org.example.project.home.presentation.screens.HomeScreen
import org.koin.compose.koinInject

@Composable
fun HomeNavigation(
    onCreatePost : () -> Unit,
    onProfileClick : () -> Unit,
    onBack: () -> Unit
) {
    val homeBackStack = rememberNavBackStack(Route.Home)
    val currentLevelManager: CurrentLevelManager = koinInject()
    val currentPostLevel by currentLevelManager.currentLevel.collectAsState()
    var isBottomBarVisible by remember { mutableStateOf(true) }

    NavDisplay(
        backStack = homeBackStack,
        onBack = { 
            if (homeBackStack.size > 1) {
                homeBackStack.removeLastOrNull()
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
            entry<Route.Home> {

                Scaffold(
                    bottomBar = {
                        if (isBottomBarVisible) {
                            BottomNavigationBar(
                                currentLevel = currentPostLevel,
                                onLevelChange = { postLevel ->
                                    currentLevelManager.updateLevel(postLevel)
                                }
                            )
                        }
                    }
                ) { paddingValues ->
                    HomeScreen(
                        onNavigateToCreatePost = onCreatePost,
                        onNavigateToProfile = onProfileClick,
                        onExpandedPostChange = { isExpanded ->
                            isBottomBarVisible = !isExpanded
                        },
                        modifier = Modifier.padding(paddingValues)
                    )
                }

            }
        }
    )
}