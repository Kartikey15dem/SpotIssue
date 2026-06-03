package org.example.project.home.di

import org.example.project.home.presentation.CurrentLevelManager
import org.example.project.home.presentation.viewmodel.HomeViewModel
import org.example.project.home.presentation.viewmodel.PostDetailViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Android-specific Home module for ViewModels
 * Contains only Android UI layer dependencies (ViewModels)
 */
val homeUiModule = module {
    // Presentation layer - ViewModels
    viewModelOf(::HomeViewModel)
    viewModel { parameters -> PostDetailViewModel(postId = parameters.get(), postRepository = get()) }
    single { CurrentLevelManager() }
}
