package org.example.project.auth.di

import org.example.project.auth.presentation.viewmodel.AuthViewModel
import org.example.project.auth.presentation.viewmodel.LocationFetchViewModel
import org.example.project.auth.presentation.viewmodel.NameCaptureViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val authModule = module {
    viewModelOf(::AuthViewModel)
    viewModelOf(::LocationFetchViewModel)
    viewModelOf(::NameCaptureViewModel)
}
