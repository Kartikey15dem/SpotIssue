package org.example.project.feature.createPost.di

import org.example.project.feature.createPost.viewmodel.CreatePostViewModel
import org.koin.dsl.module


val createPostModule = module {
    single {
        CreatePostViewModel(get(), get(), get())
    }
}