package org.example.project.home.data.local

import org.example.project.core.data.local.AppDatabase
import org.example.project.core.data.local.entities.ActiveIssuesEntity
import org.example.project.core.data.local.entities.CacheMetadataEntity
import org.example.project.core.data.local.entities.toEntity
import org.example.project.core.data.local.entities.toPost
import org.example.project.home.domain.models.Post
import org.example.project.home.domain.models.PostLevel

import kotlin.time.Clock

/**
 * Local data source for home/feed feature
 * Handles caching of posts and active issues count
 */
class FeedLocalDataSource(private val database: AppDatabase) {

    private val postDao = database.postDao()
    private val cacheMetadataDao = database.cacheMetadataDao()
    private val activeIssuesDao = database.activeIssuesDao()

    /**
     * Get cached posts for a specific level
     */
    suspend fun getCachedPosts(postLevel: PostLevel): List<Post> {
        return postDao.getPostsByLevel(postLevel.name).map { it.toPost() }
    }

    /**
     * Cache posts for a specific level
     */
    suspend fun cachePosts(postLevel: PostLevel, posts: List<Post>) {
        val now = kotlin.time.Clock.System.now().toEpochMilliseconds()

        // Delete old posts for this level
        postDao.deletePostsByLevel(postLevel.name)

        // Insert new posts
        postDao.insertPosts(posts.map { it.toEntity(now) })

        // Update cache metadata
        val metadata = CacheMetadataEntity(
            cacheKey = CacheMetadataEntity.postsKey(postLevel.name),
            lastFetchedAt = now
        )
        cacheMetadataDao.insertMetadata(metadata)
    }

    /**
     * Check if cached posts are stale
     */
    suspend fun isPostsCacheStale(postLevel: PostLevel): Boolean {
        val metadata = cacheMetadataDao.getMetadata(
            CacheMetadataEntity.postsKey(postLevel.name)
        )
        return metadata?.isStale() ?: true
    }

    /**
     * Get cached active issues count
     */
    suspend fun getCachedActiveIssues(postLevel: PostLevel): Int? {
        return activeIssuesDao.getActiveIssues(postLevel.name)?.count
    }

    /**
     * Cache active issues count
     */
    suspend fun cacheActiveIssues(postLevel: PostLevel, count: Int) {
        val now = Clock.System.now().toEpochMilliseconds()

        val entity = ActiveIssuesEntity(
            postLevel = postLevel.name,
            count = count,
            cachedAt = now
        )
        activeIssuesDao.insertActiveIssues(entity)

        // Update cache metadata
        val metadata = CacheMetadataEntity(
            cacheKey = CacheMetadataEntity.activeIssuesKey(postLevel.name),
            lastFetchedAt = now
        )
        cacheMetadataDao.insertMetadata(metadata)
    }

    /**
     * Check if active issues cache is stale
     */
    suspend fun isActiveIssuesCacheStale(postLevel: PostLevel): Boolean {
        val metadata = cacheMetadataDao.getMetadata(
            CacheMetadataEntity.activeIssuesKey(postLevel.name)
        )
        return metadata?.isStale() ?: true
    }

    /**
     * Clear all cached data
     */
    suspend fun clearAllCache() {
        postDao.clearAll()
        activeIssuesDao.clearAll()
        cacheMetadataDao.clearAll()
    }
}

