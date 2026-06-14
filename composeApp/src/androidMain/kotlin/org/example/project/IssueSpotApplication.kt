package org.example.project

import android.app.Application
import org.example.project.feature.auth.di.authModule
import org.example.project.core.database.initializeDatabase
import org.example.project.core.di.platformModule
import org.example.project.core.utils.initializeKoin
import org.example.project.feature.createPost.di.createPostModule
import org.example.project.feature.home.di.homeModule
import org.example.project.feature.profile.di.profileModule
import org.koin.android.ext.koin.androidContext

class IssueSpotApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        val androidModules = listOf(
            authModule,
            homeModule,
            profileModule,
            createPostModule,
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
