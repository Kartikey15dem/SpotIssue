package org.example.project.core.datastore.di

import com.russhwolf.settings.Settings
import org.example.project.core.datastore.UserPreferencesDataSource
import org.example.project.core.datastore.UserPreferencesRepository
import org.example.project.core.datastore.UserPreferencesRepositoryImpl
import org.koin.dsl.module

val preferencesModule = module {

    single<Settings> { Settings() }

    single { UserPreferencesDataSource(settings = get()) }

    single<UserPreferencesRepository> {
        UserPreferencesRepositoryImpl(localDataSource = get())
    }
}