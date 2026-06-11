package org.example.project.profile.di

import org.example.project.profile.presentation.viewmodel.EditProfileViewModel
import org.example.project.profile.presentation.viewmodel.ProfileViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val profileModule = module {
    viewModelOf(::ProfileViewModel)
    viewModelOf(::EditProfileViewModel)
}
