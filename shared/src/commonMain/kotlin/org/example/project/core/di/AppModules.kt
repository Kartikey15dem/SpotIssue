package org.example.project.core.di

import org.koin.dsl.module
import org.example.project.core.network.di.supabaseModule
import org.example.project.core.data.di.RepositoryModule
import org.example.project.core.domain.di.useCaseModule
import org.example.project.home.di.homeModule
import org.example.project.profile.di.profileModule
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
    supabaseModule,

    // Core (new pattern)
    RepositoryModule,
    useCaseModule,

    // Feature modules
    homeModule,
    profileModule
)

