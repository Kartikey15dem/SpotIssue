package org.example.project.auth.di

import org.example.project.auth.data.repository.AuthRepositoryImpl
import org.example.project.auth.domain.repository.AuthRepository
import org.example.project.auth.domain.usecase.SendOtpUseCase
import org.example.project.auth.domain.usecase.VerifyOtpUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Shared Auth module - contains business logic only
 * ViewModels are in Android-specific androidAuthModule
 */
val authModule = module {
    // Repository
    singleOf(::AuthRepositoryImpl) bind AuthRepository::class

    // Use Cases
    factoryOf(::SendOtpUseCase)
    factoryOf(::VerifyOtpUseCase)

}
