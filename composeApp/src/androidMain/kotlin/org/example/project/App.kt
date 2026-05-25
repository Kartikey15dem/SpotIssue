package org.example.project

import androidx.compose.runtime.*
import coil3.compose.setSingletonImageLoaderFactory
import org.example.project.core.components.OverlayProvider
import org.example.project.core.datastore.UserPreferencesDataSource
import org.example.project.core.navigation.NavigationRoot
import org.example.project.core.navigation.Route
import org.example.project.theme.IssueSpotTheme
import org.example.project.utils.createImageLoader
import org.koin.compose.koinInject

@Composable
fun App() {
    setSingletonImageLoaderFactory { context ->
        createImageLoader(context)
    }

    IssueSpotTheme {
        val userPreferences: UserPreferencesDataSource = koinInject()

        val userData by userPreferences.userData.collectAsState()

        val start = if (userData.isLoggedIn) Route.Auth.LocationFetch else Route.Auth

        OverlayProvider {
            NavigationRoot(
                start = start
            )
        }
    }

}