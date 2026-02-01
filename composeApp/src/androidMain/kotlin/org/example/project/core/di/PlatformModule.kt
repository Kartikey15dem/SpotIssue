package org.example.project.core.di

import android.content.Context
import org.example.project.core.settings.AuthSettings
import org.koin.core.module.Module
import org.koin.dsl.module

val platformModule: Module = module {
    // Platform-specific dependencies for Android
    single<ImagePicker> { ImagePicker(get()) }
    single{ AuthSettings() }

}

class ImagePicker(context : Context){

}
