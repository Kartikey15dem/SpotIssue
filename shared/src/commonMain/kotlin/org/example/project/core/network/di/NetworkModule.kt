package org.example.project.core.network.di

import org.example.project.core.network.KtorfitClient
import org.example.project.core.network.ktorHttpClient
import org.koin.dsl.module

val networkModule = module {
    single { ktorHttpClient }
    single {
        KtorfitClient.builder()
            .httpClient(get())
            .baseURL("https://api.example.com/") // Assume base URL
            .build()
    }
    single { get<KtorfitClient>().authenticationApi }
    single { get<KtorfitClient>().homeApi }
    single { get<KtorfitClient>().profileApi }
    single { get<KtorfitClient>().postApi }
}
