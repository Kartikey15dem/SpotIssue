package org.example.project.core.settings

import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set

// Android actual implementation backed by Multiplatform Settings (no-arg)
actual class AuthSettings actual constructor() {
    private val settings: Settings = Settings()

    private companion object {
        const val KEY_IS_LOGGED_IN = "is_logged_in"
        const val KEY_FIRST_PROFILE_LOAD = "first_profile_load"
        const val KEY_LAST_PROFILE_SYNC = "last_profile_sync"
    }

    actual fun isLoggedIn(): Boolean = settings[KEY_IS_LOGGED_IN] ?: false

    actual fun setLoggedIn(value: Boolean) {
        settings[KEY_IS_LOGGED_IN] = value
    }

    actual fun isFirstProfileLoad(): Boolean = settings[KEY_FIRST_PROFILE_LOAD] ?: true

    actual fun markProfileLoaded() {
        settings[KEY_FIRST_PROFILE_LOAD] = false
    }

    actual fun getLastProfileSync(): Long = settings[KEY_LAST_PROFILE_SYNC] ?: 0L

    actual fun setLastProfileSync(timestamp: Long) {
        settings[KEY_LAST_PROFILE_SYNC] = timestamp
    }
}

