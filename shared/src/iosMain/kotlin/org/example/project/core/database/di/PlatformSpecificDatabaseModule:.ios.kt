package org.example.project.core.database.di

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.example.project.core.database.AppDatabaseFactory
import org.example.project.core.database.IssueSpotDatabase
import org.koin.core.module.Module
import org.koin.dsl.module


actual val platformDatabaseModule: Module = module{
    single<IssueSpotDatabase> {

        AppDatabaseFactory()
            .createDatabase<IssueSpotDatabase>("issuespot.db")
            .fallbackToDestructiveMigration(true)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
    }

}