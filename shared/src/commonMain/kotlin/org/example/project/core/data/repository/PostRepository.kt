package org.example.project.core.data.repository

import org.example.project.home.domain.models.CreatePost

interface PostRepository {
    suspend fun likePost(postId: String): Result<Unit>
    suspend fun reportPost(postId: String, reason: String?): Result<Unit>
    suspend fun sharePost(postId: String): Result<Unit>
    suspend fun addComment(postId: String, comment: String): Result<Unit>
    suspend fun createPost(post : CreatePost): Result<Unit>
    suspend fun deletePost(postId: String):Result<Unit>
}