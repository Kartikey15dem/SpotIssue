package org.example.project.core.data.di

import org.example.project.core.data.local.FeedLocalDataSource
import org.example.project.core.data.local.ProfileLocalDataSource
import org.example.project.core.data.repository.AuthRepository
import org.example.project.core.data.repository.FeedRepository
import org.example.project.core.data.repository.PostRepository
import org.example.project.core.data.repository.ProfileRepository
import org.example.project.core.data.repositoryImp.AuthRepositoryImpl
import org.example.project.core.data.repositoryImp.FeedRepositoryImpl
import org.example.project.core.data.repositoryImp.PostRepositoryImpl
import org.example.project.core.data.repositoryImp.ProfileRepositoryImpl
import org.koin.dsl.bind
import org.koin.dsl.module

val RepositoryModule = module {
    // Auth
    single { AuthRepositoryImpl(get(), get(), get()) } bind AuthRepository::class

    // Feed
    factory { FeedRepositoryImpl(get(), get(), get(), get()) } bind FeedRepository::class

    // Post
    single { PostRepositoryImpl(get(), get(), get()) } bind PostRepository::class

    // Profile
    single { ProfileRepositoryImpl(get(), get(), get(), get(), get()) } bind ProfileRepository::class

    single { FeedLocalDataSource(get()) }
    single { ProfileLocalDataSource(get()) }
}
