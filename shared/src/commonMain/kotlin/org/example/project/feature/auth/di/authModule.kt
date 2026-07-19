package org.example.project.feature.auth.di

import org.example.project.feature.auth.viewmodel.AuthViewModel
import org.example.project.feature.auth.viewmodel.LocationFetchViewModel
import org.example.project.feature.auth.viewmodel.NameCaptureViewModel
import org.koin.dsl.module

val authModule =
    module {
        single {
            AuthViewModel(get(), get(), get())
        }
        factory { (email: String) ->
            NameCaptureViewModel(email, get(), get())
        }
        factory {
            LocationFetchViewModel(get(), get())
        }
    }
