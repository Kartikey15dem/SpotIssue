package org.example.project.createPost.di

import org.example.project.createPost.presentation.viewmodel.CreatePostViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val createPostUiModule = module {
    viewModelOf(::CreatePostViewModel)
}