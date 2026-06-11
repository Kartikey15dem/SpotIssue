package org.example.project.core.database.di

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import org.example.project.core.database.AppDatabaseFactory
import org.example.project.core.database.IssueSpotDatabase
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.Module
import org.koin.dsl.module
import kotlin.coroutines.CoroutineContext
import kotlin.jvm.java

actual val platformDatabaseModule: Module = module{
    single<IssueSpotDatabase> {
        val ioContext: CoroutineContext = Dispatchers.IO

        AppDatabaseFactory(androidApplication())
            .createDatabase(IssueSpotDatabase::class.java, "issuespot.db")
            .fallbackToDestructiveMigration(true)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(ioContext)
            .build()
    }

}
