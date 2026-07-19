package org.example.project.core.network.di

import org.example.project.core.network.DarwinNetworkMonitor
import org.example.project.core.utils.NetworkMonitor
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module

actual val platformNetworkModule: Module =
    module {
        single { DarwinNetworkMonitor() } bind NetworkMonitor::class
    }
