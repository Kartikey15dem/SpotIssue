package org.example.project.core.utils

import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Helpers to keep DB ordering stable for offline-first paging.
 */
internal fun parseIsoEpochMillis(iso: String): Long {
    return try {
        Instant.parse(iso).toEpochMilliseconds()
    } catch (_: Throwable) {
        Clock.System.now().toEpochMilliseconds()
    }
}

