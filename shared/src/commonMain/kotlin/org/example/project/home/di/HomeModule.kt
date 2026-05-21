package org.example.project.home.di

import org.example.project.home.data.local.FeedLocalDataSource
import org.example.project.home.data.repository.FakeFeedRepositoryImpl
import org.example.project.core.data.repositoryImp.FakePostRepositoryImpl
import org.example.project.core.data.repository.FeedRepository
import org.example.project.core.data.repository.PostRepository
import org.example.project.home.domain.usecases.GetActiveIssuesUseCase
import org.example.project.home.domain.usecases.GetCachedActiveIssuesUseCase
import org.example.project.home.domain.usecases.GetCachedPostsUseCase
import org.example.project.home.domain.usecases.GetPostsUseCase
import org.example.project.home.domain.usecases.IsCacheStaleUseCase
import org.example.project.home.domain.usecases.PostActionsUseCase
import org.example.project.home.domain.usecases.RefreshPostsUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Shared Home module - contains business logic only
 * ViewModels are in Android-specific androidHomeModule
 */
val homeModule = module {
    // Singleton manager for current post level


    // Local data source for caching
    single { FeedLocalDataSource(get()) }

    // Remote data source for Supabase API (commented out - we fake the API calls)
    // single { FeedRemoteDataSource(get()) }

    // Data layer - repositories - USING FAKE IMPLEMENTATION WITH REAL LOCAL DATA
    single { FakeFeedRepositoryImpl(get()) } bind FeedRepository::class
    singleOf(::FakePostRepositoryImpl) bind PostRepository::class

    // Real implementations (commented out)
    // single { FeedRepositoryImpl(get(), get()) } bind FeedRepository::class

    // Domain layer - use cases
    factoryOf(::GetPostsUseCase)
    factoryOf(::GetCachedPostsUseCase)
    factoryOf(::GetActiveIssuesUseCase)
    factoryOf(::GetCachedActiveIssuesUseCase)
    factoryOf(::RefreshPostsUseCase)
    factoryOf(::IsCacheStaleUseCase)
    factoryOf(::PostActionsUseCase)
}

