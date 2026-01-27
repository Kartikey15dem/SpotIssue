package org.example.project.auth.di

import org.example.project.auth.presentation.LocationPermissionHandler
import org.example.project.auth.presentation.viewmodel.AuthViewModel
import org.example.project.auth.presentation.viewmodel.LocationFetchViewModel
import org.example.project.auth.presentation.viewmodel.NameCaptureViewModel
import org.example.project.utils.LocationProvider
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Android-specific Auth module for ViewModels
 * Contains only Android UI layer dependencies (ViewModels)
 */
val authUiModule = module {
    // ViewModels with dependencies
    viewModelOf(::AuthViewModel)
    viewModelOf(::LocationFetchViewModel)
    viewModelOf(::NameCaptureViewModel)
    single { LocationPermissionHandler(get()) }
    single{ LocationProvider(get()) }
}
