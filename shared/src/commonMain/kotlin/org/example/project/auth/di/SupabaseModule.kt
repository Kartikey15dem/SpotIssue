package org.example.project.auth.di

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth

import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.logging.LogLevel
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import org.example.project.core.Secrets.SUPABASE_ANON_KEY
import org.example.project.core.Secrets.SUPABASE_URL

import org.koin.dsl.module

val supabaseModule = module {
    single {
        createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_ANON_KEY
        ) {
            defaultLogLevel = LogLevel.DEBUG

            install(Postgrest)

            install(Auth) {
                // PKCE flow for Android
                scheme = "io.jan.supabase"
                host = "login"
                flowType = FlowType.PKCE
            }

            install(Realtime)
        }
    }
}