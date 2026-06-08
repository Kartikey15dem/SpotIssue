package org.example.project.profile.di

import org.example.project.profile.presentation.viewmodel.EditProfileViewModel
import org.example.project.profile.presentation.viewmodel.ProfileViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * Android-specific Profile module for ViewModels
 * Contains only Android UI layer dependencies (ViewModels)
 */
val profileUiModule = module {
    // Presentation layer - ViewModels
    viewModelOf(::ProfileViewModel)
    viewModelOf(::EditProfileViewModel)
}
