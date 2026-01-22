package org.example.project.home.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import org.example.project.core.navigation.Route
import org.example.project.home.presentation.CurrentLevelManager
import org.example.project.home.presentation.screens.HomeScreen
import org.koin.compose.koinInject

@Composable
fun HomeNavigation(
    modifier : Modifier = Modifier,
    onCreatePost : () -> Unit,
    onProfileClick : () -> Unit
) {
    val homeBackStack = rememberNavBackStack(Route.Home)
    val currentLevelManager: CurrentLevelManager = koinInject()
    val currentPostLevel by currentLevelManager.currentLevel.collectAsState()

    NavDisplay(
        backStack = homeBackStack,
        onBack = { homeBackStack.removeLastOrNull()},
        entryProvider = entryProvider {
            entry<Route.Home> {

                Scaffold(
                    bottomBar = {
                        BottomNavigationBar(
                            currentLevel = currentPostLevel,
                            onLevelChange = { postLevel ->
                                currentLevelManager.updateLevel(postLevel)
                            }
                        )
                    }
                ) { paddingValues ->
                    HomeScreen(
                        onNavigateToCreatePost = onCreatePost,
                        onNavigateToProfile = onProfileClick,
                        modifier = Modifier.padding(paddingValues)
                    )
                }

            }
        }
    )
}