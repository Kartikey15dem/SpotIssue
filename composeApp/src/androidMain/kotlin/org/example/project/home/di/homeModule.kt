package org.example.project.home.di

import org.example.project.home.presentation.CurrentLevelManager
import org.example.project.home.presentation.viewmodel.HomeViewModel
import org.example.project.home.presentation.viewmodel.PostDetailViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val homeModule = module {

    viewModelOf(::HomeViewModel)
    viewModel { parameters -> PostDetailViewModel(postId = parameters.get(), postRepository = get()) }
    single { CurrentLevelManager() }
}
