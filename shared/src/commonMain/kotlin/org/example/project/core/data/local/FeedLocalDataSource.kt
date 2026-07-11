package org.example.project.core.data.local

import androidx.room.useWriterConnection
import androidx.room.immediateTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.example.project.core.database.IssueSpotDatabase
import org.example.project.core.database.entities.ActiveIssuesEntity
import org.example.project.core.database.entities.CacheMetadataEntity
import org.example.project.core.data.mappers.toPost
import org.example.project.core.database.entities.toEntity
import org.example.project.core.database.entities.toPost
import org.example.project.core.model.home.Post
import org.example.project.core.model.home.PostLevel
import kotlin.time.Clock

/**
 * Local data source for home/feed feature
 * The single source of truth and only component allowed to access Feed-related DAOs.
 * Does not contain business logic or cache policies.
 */
class FeedLocalDataSource(private val database: IssueSpotDatabase) {

    private val postDao = database.postDao()
    private val cacheMetadataDao = database.cacheMetadataDao()
    private val activeIssuesDao = database.activeIssuesDao()
    
    private val maxCacheSize = 1000

    fun observeNewestPosts(postLevel: PostLevel, limit: Int): Flow<List<Post>> {
        println("[LOCAL] Observe (Newest) Limit: $limit")
        return postDao.observeNewestByLevel(postLevel.name, limit).map { entities ->
            entities.map { it.toPost() }
        }
    }

    fun observePostsAfterAnchor(postLevel: PostLevel, anchorCreatedAt: Long, anchorId: String, limit: Int): Flow<List<Post>> {
        println("[LOCAL] Observe (After Anchor) Limit: $limit")
        return postDao.observeAfterAnchorByLevel(postLevel.name, anchorCreatedAt, anchorId, limit).map { entities ->
            entities.map { it.toPost() }
        }
    }

    suspend fun replacePosts(postLevel: PostLevel, posts: List<Post>) {
        println("[LOCAL] Replace")
        database.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                postDao.deletePostsByLevel(postLevel.name)
                postDao.insertPosts(posts.map { it.toEntity(cachedAt = it.createdAt) })
                updateMetadata(postLevel)
                trimPosts(postLevel)
            }
        }
    }

    suspend fun appendPosts(postLevel: PostLevel, posts: List<Post>) {
        println("[LOCAL] Append")
        database.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                postDao.insertPosts(posts.map { it.toEntity(cachedAt = it.createdAt) })
                updateMetadata(postLevel)
            }
        }
    }

    suspend fun clearPosts(postLevel: PostLevel) {
        postDao.deletePostsByLevel(postLevel.name)
    }

    suspend fun trimPosts(postLevel: PostLevel) {
        println("[LOCAL] Trim")
        postDao.trimPostsByLevel(postLevel.name, maxCacheSize)
    }

    suspend fun updateMetadata(postLevel: PostLevel) {
        println("[LOCAL] Metadata")
        val now = Clock.System.now().toEpochMilliseconds()
        val metadata = CacheMetadataEntity(
            cacheKey = CacheMetadataEntity.postsKey(postLevel.name),
            lastFetchedAt = now
        )
        cacheMetadataDao.insertMetadata(metadata)
    }

    suspend fun getCachedPostCount(postLevel: PostLevel): Int {
        return postDao.getPostCountByLevel(postLevel.name)
    }

    fun observeActiveIssues(postLevel: PostLevel): Flow<Int?> {
        return activeIssuesDao.observeActiveIssues(postLevel.name).map { it?.count }
    }

    suspend fun cacheActiveIssues(postLevel: PostLevel, count: Int) {
        val now = Clock.System.now().toEpochMilliseconds()

        val entity = ActiveIssuesEntity(
            postLevel = postLevel.name,
            count = count,
            cachedAt = now
        )
        database.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                activeIssuesDao.insertActiveIssues(entity)
                val metadata = CacheMetadataEntity(
                    cacheKey = CacheMetadataEntity.activeIssuesKey(postLevel.name),
                    lastFetchedAt = now
                )
                cacheMetadataDao.insertMetadata(metadata)
            }
        }
    }
}