package org.example.project.feature.home.di


import org.example.project.feature.home.CurrentLevelManager
import org.example.project.feature.home.viewmodel.HomeViewModel
import org.koin.dsl.module

val homeModule = module {
    single { CurrentLevelManager() }
    factory {
        HomeViewModel(get(), get(), get(), get(), get())
    }
}
