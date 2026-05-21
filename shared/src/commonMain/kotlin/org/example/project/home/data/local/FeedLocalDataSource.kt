package org.example.project.home.data.local

import org.example.project.core.database.IssueSpotDatabase
import org.example.project.core.database.entities.ActiveIssuesEntity
import org.example.project.core.database.entities.CacheMetadataEntity
import org.example.project.core.database.entities.toEntity
import org.example.project.core.database.entities.toPost
import org.example.project.home.domain.models.Post
import org.example.project.home.domain.models.PostLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

import kotlin.time.Clock

/**
 * Local data source for home/feed feature
 * Handles caching of posts and active issues count
 */
class FeedLocalDataSource(private val database: IssueSpotDatabase) {

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
     * Observe cached posts for a specific level
     */
    fun observeCachedPosts(postLevel: PostLevel): Flow<List<Post>> {
        return postDao.observePostsByLevel(postLevel.name).map { list ->
            list.map { it.toPost() }
        }
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
     * Observe cached active issues count
     */
    fun observeCachedActiveIssues(postLevel: PostLevel): Flow<Int?> {
        return activeIssuesDao.observeActiveIssues(postLevel.name).map { it?.count }
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

