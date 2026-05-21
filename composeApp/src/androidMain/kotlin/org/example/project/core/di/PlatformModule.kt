package org.example.project.core.di

import android.content.Context
import org.example.project.utils.AndroidImagePicker
import org.example.project.utils.AndroidVideoPicker
import org.koin.core.module.Module
import org.koin.dsl.module

val platformModule: Module = module {
    // Platform-specific dependencies for Android
    single { AndroidImagePicker(get<Context>()) }
    single { AndroidVideoPicker(get<Context>()) }
}
