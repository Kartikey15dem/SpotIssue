package org.example.project.core.data.mappers

import kotlin.time.Clock
import kotlin.time.Instant


private fun String.toTimeAgo(): String {
    return try {
        val postTime = Instant.parse(this)
        val now = Clock.System.now()
        val duration = now - postTime

        val totalSeconds = duration.inWholeSeconds
        val totalMinutes = totalSeconds / 60
        val totalHours = totalMinutes / 60
        val totalDays = totalHours / 24

        when {
            totalMinutes < 1 -> "Just now"
            totalHours < 1 -> "${totalMinutes} minute${if (totalMinutes > 1) "s" else ""} ago"
            totalDays < 1 -> "${totalHours} hour${if (totalHours > 1) "s" else ""} ago"
            totalDays < 7 -> "${totalDays} day${if (totalDays > 1) "s" else ""} ago"
            totalDays < 30 -> "${totalDays / 7} week${if (totalDays / 7 > 1) "s" else ""} ago"
            else -> "${totalDays / 30} month${if (totalDays / 30 > 1) "s" else ""} ago"
        }
    } catch (_: Exception) {
        "Unknown"
    }
}

