package org.example.project

import android.app.Application
import org.example.project.core.database.initializeDatabase
import org.example.project.core.di.platformModule
import org.example.project.core.utils.initializeKoin
import org.koin.android.ext.koin.androidContext

class IssueSpotApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val androidModules =
            listOf(
                platformModule,
            )
        initializeDatabase(this)

        initializeKoin(
            additionalModules = androidModules,
        ) {
            androidContext(this@IssueSpotApplication)
        }
    }
}
