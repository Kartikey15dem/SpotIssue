package org.example.project.core.di

import org.example.project.core.utils.LocationProvider
import org.example.project.utils.location.LocationPermissionHandler
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

val platformModule: Module =
    module {
        factory { LocationPermissionHandler(androidContext()) }
        single { LocationProvider() }
    }
