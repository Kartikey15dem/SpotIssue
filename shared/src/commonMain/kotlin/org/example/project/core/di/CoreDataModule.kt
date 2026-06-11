package org.example.project.core.di

import org.example.project.core.database.di.DaoModule
import org.example.project.core.data.local.FeedLocalDataSource
import org.example.project.core.data.local.ProfileLocalDataSource
import org.example.project.core.network.di.platformNetworkModule
import org.koin.dsl.module

/**
 * Core data module - contains database and core data dependencies
 */
val coreDataModule = module {
    includes(DaoModule)
    includes(platformNetworkModule)

    single { FeedLocalDataSource(get()) }
    single { ProfileLocalDataSource(get()) }
}
