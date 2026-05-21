package org.example.project.core.di

import org.koin.dsl.module
import org.example.project.core.network.di.networkModule
import org.example.project.core.data.di.RepositoryModule
import org.koin.core.module.Module


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
val appModules = listOf(
    coreDataModule,
    networkModule,

    // Core (new pattern)
    RepositoryModule,
    // useCaseModule // Commenting out as it seems to be missing or will be removed
)


