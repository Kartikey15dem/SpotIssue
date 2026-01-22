package org.example.project.core

import org.example.project.shared.BuildConfig

actual object Secrets {
    actual val SUPABASE_URL: String
        get() = BuildConfig.SUPABASE_URL

    actual val SUPABASE_ANON_KEY: String
        get() = BuildConfig.SUPABASE_ANON_KEY
}

