package org.example.project.core.utils

import org.example.project.core.database.di.platformDatabaseModule
import org.example.project.core.network.di.platformNetworkModule
import org.example.project.core.platformModule
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

fun koinInit(){
    initializeKoin(
        additionalModules = listOf(platformModule, platformDatabaseModule, platformNetworkModule)
    )

}