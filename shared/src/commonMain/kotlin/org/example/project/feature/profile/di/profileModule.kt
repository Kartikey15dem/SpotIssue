package org.example.project.feature.profile.di

import org.example.project.feature.profile.viewmodel.CounterViewModel
import org.example.project.feature.profile.viewmodel.EditProfileViewModel
import org.example.project.feature.profile.viewmodel.ProfileViewModel
import org.koin.dsl.module

val profileModule = module {
    single {
        EditProfileViewModel(get())
    }
    single {
        ProfileViewModel(get(), get())
    }
    factory {
        CounterViewModel()
    }
}
