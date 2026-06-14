package org.example.project.feature.auth.di

import org.example.project.feature.auth.viewmodel.AuthViewModel
import org.koin.dsl.module

val authModule = module {
    single {
        AuthViewModel(get(), get(), get(),)
    }
}