package org.example.project

import android.app.Application
import org.example.project.auth.di.authUiModule
import org.example.project.core.data.local.initializeDatabase
import org.example.project.core.di.platformModule
import org.example.project.home.di.homeUiModule
import org.example.project.profile.di.profileUiModule
import org.example.project.utils.initializeKoin
import org.koin.android.ext.koin.androidContext

class IssueSpotApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Koin DI using shared initializer with Android-specific modules
        val androidModules = listOf(
            authUiModule,
            homeUiModule,
            profileUiModule,
            platformModule
        )
        initializeDatabase(this)

        initializeKoin(
            additionalModules = androidModules
        ) {
            androidContext(this@IssueSpotApplication)

        }
    }
}
