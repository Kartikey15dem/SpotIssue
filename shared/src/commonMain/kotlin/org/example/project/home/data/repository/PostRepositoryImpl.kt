package org.example.project.home.data.repository

import org.example.project.home.domain.repository.PostRepository

/**
 * Fake implementation of PostRepository for development/testing
 * TODO: Replace with actual implementation when backend is ready
 */
class FakePostRepositoryImpl : PostRepository {
    override suspend fun likePost(postId: String): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun reportPost(postId: String, reason: String?): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun sharePost(postId: String): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun addComment(postId: String, comment: String): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun deletePost(postId: String): Result<Unit> {
        return Result.success(Unit)
    }
}

