package org.example.project.core.network.di

import org.example.project.core.network.IosNetworkMonitor
import org.example.project.core.network.NetworkMonitor
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module

actual val platformNetworkModule: Module = module {
    single { IosNetworkMonitor() } bind NetworkMonitor::class
}

