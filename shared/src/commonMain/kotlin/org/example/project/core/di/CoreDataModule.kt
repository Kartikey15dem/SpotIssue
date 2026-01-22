package org.example.project.core.di

import org.example.project.core.data.local.getDatabase
import org.koin.dsl.module

/**
 * Core data module - contains database and core data dependencies
 */
val coreDataModule = module {
    // Room Database
    single { getDatabase() }
}

