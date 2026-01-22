package org.example.project.core.di

import org.koin.dsl.module
import org.example.project.auth.di.authModule
import org.example.project.auth.di.supabaseModule
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

    // Feature modules
    authModule,
    homeModule,
    profileModule
)



