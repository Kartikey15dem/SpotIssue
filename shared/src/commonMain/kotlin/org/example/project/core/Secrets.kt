package org.example.project.core

/**
 * Multiplatform secrets accessor.
 * Provide platform-specific `actual` implementations in androidMain / iosMain.
 */
expect object Secrets {
    val SUPABASE_URL: String
    val SUPABASE_ANON_KEY: String
}

