package org.example.project.core.di

import org.example.project.utils.AndroidImagePicker
import org.example.project.utils.AndroidVideoPicker
import org.example.project.utils.location.LocationPermissionHandler
import org.example.project.utils.location.LocationProvider
import org.koin.core.module.Module
import org.koin.dsl.module

val platformModule: Module = module {
    single { AndroidImagePicker(get()) }
    single { AndroidVideoPicker(get()) }
    factory { LocationPermissionHandler(get()) }
    single { LocationProvider(get()) }
}
