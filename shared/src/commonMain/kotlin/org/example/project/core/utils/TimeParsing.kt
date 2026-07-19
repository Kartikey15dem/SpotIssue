package org.example.project.core.utils

import kotlinx.datetime.Instant
import kotlin.time.Clock

/**
 * Helpers to keep DB ordering stable for offline-first paging.
 */
fun parseIsoEpochMillis(iso: String?): Long {
    if (iso == null) return Clock.System.now().toEpochMilliseconds()
    return try {
        Instant.parse(iso).toEpochMilliseconds()
    } catch (e: Exception) {
        try {
            if (iso != null && !iso.endsWith("Z") && !iso.contains("+")) {
                Instant.parse(iso + "Z").toEpochMilliseconds()
            } else {
                Clock.System.now().toEpochMilliseconds()
            }
        } catch (_: Exception) {
            Clock.System.now().toEpochMilliseconds()
        }
    }
}

fun getRelativeTime(isoString: String): String {
    val epochMillis = parseIsoEpochMillis(isoString)
    val now = Clock.System.now().toEpochMilliseconds()
    val diff = now - epochMillis

    val seconds = diff / 1000L
    val minutes = seconds / 60L
    val hours = minutes / 60L
    val days = hours / 24L

    return when {
        diff < 0L -> "Just now"
        seconds < 60L -> "Just now"
        minutes < 60L -> "$minutes minutes ago"
        hours < 24L -> "$hours hours ago"
        days < 7L -> "$days days ago"
        else -> "${days / 7L} weeks ago"
    }
}
