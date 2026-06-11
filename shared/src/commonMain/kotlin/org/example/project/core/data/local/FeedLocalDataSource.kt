package org.example.project.core.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.example.project.core.database.IssueSpotDatabase
import org.example.project.core.database.entities.ActiveIssuesEntity
import org.example.project.core.database.entities.CacheMetadataEntity
import org.example.project.core.database.entities.toEntity
import org.example.project.core.data.mappers.toPost
import org.example.project.core.database.entities.toPost
import org.example.project.core.model.home.Post
import org.example.project.core.model.home.PostLevel
import kotlin.time.Clock

/**
 * Local data source for home/feed feature
 * Handles caching of posts and active issues count
 */
class FeedLocalDataSource(private val database: IssueSpotDatabase) {

    private val postDao = database.postDao()
    private val cacheMetadataDao = database.cacheMetadataDao()
    private val activeIssuesDao = database.activeIssuesDao()

    suspend fun cachePosts(postLevel: PostLevel, posts: List<Post>) {
        val now = Clock.System.now().toEpochMilliseconds()

        // Delete old posts for this level
        postDao.deletePostsByLevel(postLevel.name)

        // Insert new posts
        postDao.insertPosts(posts.map { it.toEntity(now) })

        // Update cache metadata
        val metadata = CacheMetadataEntity(
            cacheKey = CacheMetadataEntity.Companion.postsKey(postLevel.name),
            lastFetchedAt = now
        )
        cacheMetadataDao.insertMetadata(metadata)
    }

    /**
     * Append posts for a level without clearing existing cache.
     *
     * This is used when paging subsequent pages from the network.
     */
    suspend fun appendPosts(postLevel: PostLevel, posts: List<Post>) {
        val now = Clock.System.now().toEpochMilliseconds()

        postDao.insertPosts(posts.map { it.toEntity(now) })

        // Mark cache as "updated" so the offline list reflects that it is fresh.
        val metadata = CacheMetadataEntity(
            cacheKey = CacheMetadataEntity.Companion.postsKey(postLevel.name),
            lastFetchedAt = now,
        )
        cacheMetadataDao.insertMetadata(metadata)
    }

    /**
     * Check if cached posts are stale
     */
    suspend fun isPostsCacheStale(postLevel: PostLevel): Boolean {
        val metadata = cacheMetadataDao.getMetadata(
            CacheMetadataEntity.Companion.postsKey(postLevel.name)
        )
        return metadata?.isStale() ?: true
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
            cacheKey = CacheMetadataEntity.Companion.activeIssuesKey(postLevel.name),
            lastFetchedAt = now
        )
        cacheMetadataDao.insertMetadata(metadata)
    }

}