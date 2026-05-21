package org.example.project.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.time.Clock

/**
 * Entity to store cache metadata (timestamps) for each PostLevel
 * This helps determine if cached data is stale
 */
@Entity(tableName = "cache_metadata")
data class CacheMetadataEntity(
    @PrimaryKey
    val cacheKey: String, // e.g., "posts_LOCALITY", "active_issues_LOCALITY"
    val lastFetchedAt: Long, // Timestamp when data was last fetched from API
    val expiryDuration: Long = 2 * 60 * 1000 // Default 5 minutes in milliseconds
) {
    /**
     * Check if cache is stale based on expiry duration
     */
    fun isStale(): Boolean {
        val now = Clock.System.now().toEpochMilliseconds()
        return (now - lastFetchedAt) > expiryDuration
    }

    companion object {
        fun postsKey(postLevel: String): String = "posts_$postLevel"
        fun activeIssuesKey(postLevel: String): String = "active_issues_$postLevel"
        fun userPostsKey(userId: String): String = "user_posts_$userId"
        fun likedPostsKey(userId: String): String = "liked_posts_$userId"
    }
}

