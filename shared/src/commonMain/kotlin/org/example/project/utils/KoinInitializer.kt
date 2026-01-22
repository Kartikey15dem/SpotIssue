package org.example.project.utils

import org.example.project.core.di.appModules
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module

fun initializeKoin(
    additionalModules: List<Module> = emptyList(),
    config : (KoinApplication.() -> Unit)? = null
){
    startKoin {
        config?.invoke(this)
        modules(appModules + additionalModules)
    }
}