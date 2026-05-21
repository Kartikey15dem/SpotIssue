package org.example.project.core.data.repository

import org.example.project.home.domain.models.Post
import org.example.project.home.domain.models.PostLevel

/**
 * Home feature repository interface
 * Defines data operations for the home/feed feature
 */
interface FeedRepository {
    /**
     * Fetch posts for a given post level
     * Uses cache-first strategy: loads from cache first, then from API if needed
     */
    suspend fun getPosts(postLevel: PostLevel): Result<List<Post>>

    /**
     * Get cached posts (instant, no network call)
     */
    suspend fun getCachedPosts(postLevel: PostLevel): Result<List<Post>>

    /**
     * Force refresh posts from API (ignores cache)
     */
    suspend fun refreshPosts(postLevel: PostLevel): Result<List<Post>>

    /**
     * Check if posts cache is stale for given level
     */
    suspend fun isPostsCacheStale(postLevel: PostLevel): Boolean

    /**
     * Get active issues count for a given post level
     */
    suspend fun getActiveIssuesCount(postLevel: PostLevel): Result<Int>

    /**
     * Get cached active issues count (instant, no network call)
     */
    suspend fun getCachedActiveIssuesCount(postLevel: PostLevel): Result<Int?>

    /**
     * Force refresh active issues count from API
     */
    suspend fun refreshActiveIssuesCount(postLevel: PostLevel): Result<Int>

    /**
     * Check if active issues cache is stale for given level
     */
    suspend fun isActiveIssuesCacheStale(postLevel: PostLevel): Boolean
}