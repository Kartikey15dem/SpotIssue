package org.example.project.home.domain.usecases

import org.example.project.home.domain.models.Post
import org.example.project.home.domain.models.PostLevel
import org.example.project.home.domain.repository.FeedRepository

/**
 * Get posts - uses cache-first strategy
 * Loads from cache if fresh, otherwise fetches from API
 */
class GetPostsUseCase(
    private val feedRepository: FeedRepository
) {
    suspend operator fun invoke(postLevel: PostLevel): Result<List<Post>> {
        return feedRepository.getPosts(postLevel)
    }
}

/**
 * Get cached posts instantly (no network call)
 */
class GetCachedPostsUseCase(
    private val feedRepository: FeedRepository
) {
    suspend operator fun invoke(postLevel: PostLevel): Result<List<Post>> {
        return feedRepository.getCachedPosts(postLevel)
    }
}

/**
 * Get active issues count - uses cache-first strategy
 */
class GetActiveIssuesUseCase(
    private val feedRepository: FeedRepository
) {
    suspend operator fun invoke(postLevel: PostLevel): Result<Int> {
        return feedRepository.getActiveIssuesCount(postLevel)
    }
}

/**
 * Get cached active issues count instantly (no network call)
 */
class GetCachedActiveIssuesUseCase(
    private val feedRepository: FeedRepository
) {
    suspend operator fun invoke(postLevel: PostLevel): Result<Int?> {
        return feedRepository.getCachedActiveIssuesCount(postLevel)
    }
}

/**
 * Force refresh posts from API (ignores cache)
 */
class RefreshPostsUseCase(
    private val feedRepository: FeedRepository
) {
    suspend operator fun invoke(postLevel: PostLevel): Result<List<Post>> {
        return feedRepository.refreshPosts(postLevel)
    }
}

/**
 * Check if cache is stale for given level
 */
class IsCacheStaleUseCase(
    private val feedRepository: FeedRepository
) {
    suspend fun forPosts(postLevel: PostLevel): Boolean {
        return feedRepository.isPostsCacheStale(postLevel)
    }

    suspend fun forActiveIssues(postLevel: PostLevel): Boolean {
        return feedRepository.isActiveIssuesCacheStale(postLevel)
    }
}

