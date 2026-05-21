package org.example.project

import android.app.Application
import org.example.project.auth.di.authModule
import org.example.project.core.database.initializeDatabase
import org.example.project.core.di.platformModule
import org.example.project.core.utils.initializeKoin
import org.example.project.createPost.di.createPostUiModule
import org.example.project.home.di.homeUiModule
import org.example.project.profile.di.profileUiModule
import org.koin.android.ext.koin.androidContext

class IssueSpotApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        val androidModules = listOf(
            authModule,
            homeUiModule,
            profileUiModule,
            createPostUiModule,
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
