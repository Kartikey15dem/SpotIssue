package org.example.project.home.domain.usecases

import org.example.project.home.domain.repository.PostRepository

class PostActionsUseCase(
    private val repository: PostRepository
) {
    suspend fun like(postId: String): Result<Unit> = repository.likePost(postId)

    suspend fun report(postId: String, reason: String? = null): Result<Unit> =
        repository.reportPost(postId, reason)

    suspend fun share(postId: String): Result<Unit> = repository.sharePost(postId)

    suspend fun comment(postId: String, comment: String): Result<Unit> =
        if (comment.isBlank()) Result.failure(IllegalArgumentException("empty comment"))
        else repository.addComment(postId, comment)

    suspend fun delete(postId: String): Result<Unit> =
        repository.deletePost(postId)
}