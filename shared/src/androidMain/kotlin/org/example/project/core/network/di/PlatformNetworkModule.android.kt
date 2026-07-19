package org.example.project.core.network.di

import org.example.project.core.network.ConnectivityManagerNetworkMonitor
import org.example.project.core.utils.NetworkMonitor
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module

actual val platformNetworkModule: Module =
    module {
        single { ConnectivityManagerNetworkMonitor(androidApplication()) } bind NetworkMonitor::class
    }
