package org.example.project.profile.di

import org.example.project.core.data.local.AppDatabase
import org.example.project.core.data.local.getDatabase
import org.example.project.profile.data.local.ProfileLocalDataSource
import org.example.project.profile.data.repository.ProfileRepositoryImpl
import org.example.project.profile.domain.repository.ProfileRepository
import org.example.project.profile.domain.usecases.GetLikedPostsUseCase
import org.example.project.profile.domain.usecases.GetProfileUseCase
import org.example.project.profile.domain.usecases.GetUserPostsUseCase
import org.example.project.profile.domain.usecases.RefreshProfileUseCase
import org.example.project.profile.domain.usecases.UpdateProfileUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Shared Profile module - contains business logic only
 * ViewModels are in Android-specific androidProfileModule
 */
val profileModule = module {
    // Database - single instance
    single<AppDatabase> { getDatabase() }

    // DAOs - from database
    single { get<AppDatabase>().profileDao() }
    single { get<AppDatabase>().userPostDao() }
    single { get<AppDatabase>().likedPostDao() }

    // Local data source (requires all 3 DAOs)
    single {
        ProfileLocalDataSource(
            profileDao = get(),
            userPostDao = get(),
            likedPostDao = get()
        )
    }

    // Data layer - repositories (requires AuthSettings and SupabaseClient for cache tracking and API calls)
    single {
        ProfileRepositoryImpl(
            localDataSource = get(),
            authSettings = get(),
            supabase = get()
        )
    } bind ProfileRepository::class

    // Domain layer - use cases
    factoryOf(::GetProfileUseCase)
    factoryOf(::UpdateProfileUseCase)
    factoryOf(::GetUserPostsUseCase)
    factoryOf(::GetLikedPostsUseCase)
    factoryOf(::RefreshProfileUseCase)
}

