package org.example.project.core.utils

import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Helpers to keep DB ordering stable for offline-first paging.
 */
fun parseIsoEpochMillis(iso: String?): Long {
    if (iso == null) return Clock.System.now().toEpochMilliseconds()
    return try {
        Instant.parse(iso).toEpochMilliseconds()
    } catch (_: Throwable) {
        Clock.System.now().toEpochMilliseconds()
    }
}

fun getRelativeTime(isoString: String): String {
    val epochMillis = parseIsoEpochMillis(isoString)
    val now = Clock.System.now().toEpochMilliseconds()
    val diff = now - epochMillis
    
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    
    return when {
        diff < 0 -> "Just now"
        seconds < 60 -> "Just now"
        minutes < 60 -> "$minutes minutes ago"
        hours < 24 -> "$hours hours ago"
        days < 7 -> "$days days ago"
        else -> "${days / 7} weeks ago"
    }
}

