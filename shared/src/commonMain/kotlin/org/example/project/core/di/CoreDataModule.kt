package org.example.project.core.di

import org.example.project.core.database.di.DaoModule
import org.example.project.core.database.getDatabase
import org.example.project.home.data.local.FeedLocalDataSource
import org.example.project.profile.data.local.ProfileLocalDataSource
import org.koin.dsl.module

/**
 * Core data module - contains database and core data dependencies
 */
val coreDataModule = module {
    includes(DaoModule)
    // Room Database
    single { getDatabase() }

    // Local data sources
    single { FeedLocalDataSource(get()) }
    single { ProfileLocalDataSource(get(), get(), get()) }
}

