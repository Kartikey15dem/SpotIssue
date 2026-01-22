package org.example.project

import androidx.compose.runtime.*
import coil3.compose.setSingletonImageLoaderFactory
import org.example.project.core.navigation.NavigationRoot
import org.example.project.core.navigation.Route
import org.example.project.core.settings.AuthSettings
import org.example.project.theme.IssueSpotTheme
import org.example.project.utils.createImageLoader
import org.koin.compose.koinInject

@Composable
fun App() {
    setSingletonImageLoaderFactory { context ->
        createImageLoader(context)
    }

    IssueSpotTheme {
        val authSettings: AuthSettings = koinInject()
        val start = if (authSettings.isLoggedIn()) Route.Home else Route.Auth
        NavigationRoot(
            start = start
        )
    }

}