package org.example.project.core.di

import org.example.project.core.data.di.RepositoryModule
import org.example.project.core.database.di.DaoModule
import org.example.project.core.datastore.di.preferencesModule
import org.example.project.core.network.di.networkModule
import org.example.project.core.network.di.platformNetworkModule
import org.example.project.feature.auth.di.authModule
import org.example.project.feature.createPost.di.createPostModule
import org.example.project.feature.home.di.homeModule
import org.example.project.feature.profile.di.profileModule

/**
 * Platform-specific module (expect/actual)
 * Contains platform-specific dependencies like ImagePicker
 */

/**
 * Complete app modules list
 * Each feature module (auth, home, profile) contains its own:
 * - Data layer (repositories)
 * - Domain layer (use cases, managers)
 * - Presentation layer (ViewModels)
 */
val appModules =
    listOf(
        platformNetworkModule,
        networkModule,
        RepositoryModule,
        preferencesModule,
        DaoModule,
        authModule,
        homeModule,
        profileModule,
        createPostModule,
    )
