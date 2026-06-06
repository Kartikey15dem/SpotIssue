package org.example.project.auth.di

import org.example.project.auth.presentation.viewmodel.AuthViewModel
import org.example.project.auth.presentation.viewmodel.LocationFetchViewModel
import org.example.project.auth.presentation.viewmodel.NameCaptureViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * Android-specific Auth module for ViewModels
 * Contains only Android UI layer dependencies (ViewModels)
 */
val authModule = module {
    // ViewModels with dependencies
    viewModelOf(::AuthViewModel)
    viewModelOf(::LocationFetchViewModel)
    viewModel { params ->
        NameCaptureViewModel(
            context = get(),
            email = params.get<String>(),
            prefRepository = get(),
            profileRepository = get(),
            imagePicker = get()
        )
    }
}
