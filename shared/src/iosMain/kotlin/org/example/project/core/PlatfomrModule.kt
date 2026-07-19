package org.example.project.core

import org.example.project.core.utils.LocationProvider
import org.koin.dsl.module

val platformModule =
    module {
        single { LocationProvider() }
    }
