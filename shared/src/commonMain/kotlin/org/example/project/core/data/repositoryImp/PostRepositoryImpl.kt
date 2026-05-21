package org.example.project.core.data.repositoryImp

import org.example.project.core.data.repository.PostRepository
import org.example.project.core.network.services.PostService
import org.example.project.home.domain.models.CreatePost

class PostRepositoryImpl(
    private val postService: PostService
) : PostRepository {
    override suspend fun likePost(postId: String): Result<Unit> {
        return try {
            postService.likePost(postId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun reportPost(postId: String, reason: String?): Result<Unit> {
        return try {
            postService.reportPost(postId, reason)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sharePost(postId: String): Result<Unit> {
        // Assume sharing is local or has an endpoint
        return Result.success(Unit)
    }

    override suspend fun addComment(postId: String, comment: String): Result<Unit> {
        return try {
            postService.addComment(postId, comment)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createPost(post: CreatePost): Result<Unit> {
        return try {
            postService.createPost(post)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deletePost(postId: String): Result<Unit> {
        return try {
            postService.deletePost(postId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
