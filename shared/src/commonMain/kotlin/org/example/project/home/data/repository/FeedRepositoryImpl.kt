package org.example.project.home.data.repository

import org.example.project.home.data.local.FeedLocalDataSource
import org.example.project.home.data.remote.FeedRemoteDataSource
import org.example.project.home.data.remote.mapper.toPost
import org.example.project.home.domain.repository.FeedRepository

import co.touchlab.kermit.Logger
import org.example.project.home.domain.models.Post
import org.example.project.home.domain.models.PostLevel

/**
 * Real implementation of FeedRepository using Supabase
 * Implements caching strategy with Room DB and Supabase API
 */
class FeedRepositoryImpl(
    private val remoteDataSource: FeedRemoteDataSource,
    private val localDataSource: FeedLocalDataSource
) : FeedRepository {
    private val logger = Logger.withTag("FeedRepository")

    /**
     * Smart fetch: Uses cache if fresh, otherwise fetches from API
     */
    override suspend fun getPosts(postLevel: PostLevel): Result<List<Post>> {
        logger.d { "getPosts called for level=$postLevel" }
        return try {
            // Check if cache is stale
            val isStale = localDataSource.isPostsCacheStale(postLevel)
            logger.d { "isPostsCacheStale=$isStale for level=$postLevel" }

            if (isStale) {
                // Cache is stale or doesn't exist, fetch from API
                refreshPosts(postLevel)
            } else {
                // Use cache
                getCachedPosts(postLevel)
            }
        } catch (e: Exception) {
            logger.e(e) { "Error while getting posts for level=$postLevel" }
            // If API fails, try to return cached data as fallback
            try {
                val cachedPosts = localDataSource.getCachedPosts(postLevel)
                if (cachedPosts.isNotEmpty()) {
                    logger.d { "Returning ${cachedPosts.size} cached posts for level=$postLevel as fallback" }
                    Result.success(cachedPosts)
                } else {
                    Result.failure(e)
                }
            } catch (_: Exception) {
                logger.e(e) { "Error while fetching cached posts fallback for level=$postLevel" }
                Result.failure(e)
            }
        }
    }

    override suspend fun getCachedPosts(postLevel: PostLevel): Result<List<Post>> {
        return try {
            val cachedPosts = localDataSource.getCachedPosts(postLevel)
            logger.d { "Loaded ${cachedPosts.size} cached posts for level=$postLevel" }
            Result.success(cachedPosts)
        } catch (e: Exception) {
            logger.e(e) { "Failed to load cached posts for level=$postLevel" }
            Result.failure(e)
        }
    }

    override suspend fun refreshPosts(postLevel: PostLevel): Result<List<Post>> {
        logger.d { "Refreshing posts from remote for level=$postLevel" }
        return try {
            // Fetch from Supabase
            val postDtos = remoteDataSource.fetchPosts(postLevel)

            // Map to domain models
            val posts = postDtos.map { it.toPost() }

            // Cache the fetched posts
            localDataSource.cachePosts(postLevel, posts)
            logger.d { "Fetched and cached ${posts.size} posts for level=$postLevel" }

            Result.success(posts)
        } catch (e: Exception) {
            logger.e(e) { "Failed to refresh posts for level=$postLevel" }
            Result.failure(e)
        }
    }

    override suspend fun isPostsCacheStale(postLevel: PostLevel): Boolean {
        return localDataSource.isPostsCacheStale(postLevel)
    }

    override suspend fun getActiveIssuesCount(postLevel: PostLevel): Result<Int> {
        logger.d { "getActiveIssuesCount called for level=$postLevel" }
        return try {
            val isStale = localDataSource.isActiveIssuesCacheStale(postLevel)

            if (isStale) {
                refreshActiveIssuesCount(postLevel)
            } else {
                val cached = localDataSource.getCachedActiveIssues(postLevel)
                logger.d { "Returning cached active issues=${cached ?: 0} for level=$postLevel" }
                Result.success(cached ?: 0)
            }
        } catch (e: Exception) {
            logger.e(e) { "Error while getting active issues count for level=$postLevel" }
            // Fallback to cache if API fails
            try {
                val cached = localDataSource.getCachedActiveIssues(postLevel)
                Result.success(cached ?: 0)
            } catch (_: Exception) {
                logger.e(e) { "Error while fetching cached active issues fallback for level=$postLevel" }
                Result.failure(e)
            }
        }
    }

    override suspend fun getCachedActiveIssuesCount(postLevel: PostLevel): Result<Int?> {
        return try {
            val count = localDataSource.getCachedActiveIssues(postLevel)
            logger.d { "Loaded cached active issues=$count for level=$postLevel" }
            Result.success(count)
        } catch (e: Exception) {
            logger.e(e) { "Failed to load cached active issues for level=$postLevel" }
            Result.failure(e)
        }
    }

    override suspend fun refreshActiveIssuesCount(postLevel: PostLevel): Result<Int> {
        logger.d { "Refreshing active issues count from remote for level=$postLevel" }
        return try {
            // Fetch from Supabase
            val count = remoteDataSource.fetchActiveIssuesCount(postLevel)

            // Cache the count
            localDataSource.cacheActiveIssues(postLevel, count)

            logger.d { "Fetched and cached active issues count=$count for level=$postLevel" }
            Result.success(count)
        } catch (e: Exception) {
            logger.e(e) { "Failed to refresh active issues count for level=$postLevel" }
            Result.failure(e)
        }
    }

    override suspend fun isActiveIssuesCacheStale(postLevel: PostLevel): Boolean {
        return localDataSource.isActiveIssuesCacheStale(postLevel)
    }
}
