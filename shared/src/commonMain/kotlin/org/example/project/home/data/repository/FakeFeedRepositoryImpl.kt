package org.example.project.home.data.repository

import co.touchlab.kermit.Logger
import kotlinx.coroutines.delay
import org.example.project.home.data.local.FeedLocalDataSource
import org.example.project.home.domain.models.MediaType
import org.example.project.home.domain.models.Post
import org.example.project.home.domain.models.PostLevel
import org.example.project.core.data.repository.FeedRepository

/**
 * Fake implementation of FeedRepository that keeps REAL local data source but fakes remote API calls
 */
class FakeFeedRepositoryImpl(
    private val localDataSource: FeedLocalDataSource
    // Note: No remoteDataSource - we fake the API calls
) : FeedRepository {

    private val logger = Logger.withTag("FakeFeedRepository")

    private val mockLocalityPosts = listOf(
        Post(
            id = "1", userUrl = "", userName = "John Doe", timeAgo = "2 hours ago",
            postLevel = PostLevel.LOCALITY, location = "Main Street, Downtown",
            postText = "Road Damage - Large pothole causing traffic issues",
            mediaType = MediaType.IMAGE, mediaUrl = "", likes = 15, comments = 3
        ),
        Post(
            id = "2", userUrl = "", userName = "Jane Smith", timeAgo = "5 hours ago",
            postLevel = PostLevel.LOCALITY, location = "Park Avenue, Uptown",
            postText = "Street Light Not Working - Out for a week",
            mediaType = MediaType.IMAGE, mediaUrl = "", likes = 8, comments = 1
        )
    )

    private val mockDistrictPosts = listOf(
        Post(
            id = "3", userUrl = "", userName = "Mike Johnson", timeAgo = "1 day ago",
            postLevel = PostLevel.DISTRICT, location = "Oak Street, Midtown",
            postText = "Garbage Not Collected - Piling up for days",
            mediaType = MediaType.IMAGE, mediaUrl = "", likes = 22, comments = 7
        )
    )

    private val mockActiveIssuesCount = mapOf(
        PostLevel.LOCALITY to 25,
        PostLevel.DISTRICT to 48,
        PostLevel.STATE to 75,
        PostLevel.NATIONAL to 150
    )

    override suspend fun getPosts(postLevel: PostLevel): Result<List<Post>> {
        logger.d { "FAKE: getPosts called for level=$postLevel" }
        return try {
            val isStale = localDataSource.isPostsCacheStale(postLevel)
            if (isStale) {
                refreshPosts(postLevel)
            } else {
                getCachedPosts(postLevel)
            }
        } catch (e: Exception) {
            logger.e(e) { "FAKE: Error getting posts" }
            Result.failure(e)
        }
    }

    override suspend fun getCachedPosts(postLevel: PostLevel): Result<List<Post>> {
        return try {
            val cachedPosts = localDataSource.getCachedPosts(postLevel)
            Result.success(cachedPosts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun refreshPosts(postLevel: PostLevel): Result<List<Post>> {
        return try {
            delay(800) // Fake network delay
            val posts = getMockPosts(postLevel)
            localDataSource.cachePosts(postLevel, posts)
            Result.success(posts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isPostsCacheStale(postLevel: PostLevel): Boolean {
        return localDataSource.isPostsCacheStale(postLevel)
    }

    override suspend fun getActiveIssuesCount(postLevel: PostLevel): Result<Int> {
        return try {
            val isStale = localDataSource.isActiveIssuesCacheStale(postLevel)
            if (isStale) {
                refreshActiveIssuesCount(postLevel)
            } else {
                val cached = localDataSource.getCachedActiveIssues(postLevel)
                Result.success(cached ?: 0)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCachedActiveIssuesCount(postLevel: PostLevel): Result<Int?> {
        return try {
            val count = localDataSource.getCachedActiveIssues(postLevel)
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun refreshActiveIssuesCount(postLevel: PostLevel): Result<Int> {
        return try {
            delay(400) // Fake network delay
            val count = mockActiveIssuesCount[postLevel] ?: 0
            localDataSource.cacheActiveIssues(postLevel, count)
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isActiveIssuesCacheStale(postLevel: PostLevel): Boolean {
        return localDataSource.isActiveIssuesCacheStale(postLevel)
    }

    private fun getMockPosts(postLevel: PostLevel): List<Post> {
        return when (postLevel) {
            PostLevel.LOCALITY -> mockLocalityPosts
            PostLevel.DISTRICT -> mockDistrictPosts
            PostLevel.STATE -> emptyList()
            PostLevel.NATIONAL -> emptyList()
        }
    }
}
