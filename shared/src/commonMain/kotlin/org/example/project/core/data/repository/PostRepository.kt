package org.example.project.core.data.repository

import org.example.project.core.model.createPost.CreatePost
import org.example.project.core.utils.DataState

interface PostRepository {
    suspend fun likePost(postId: String): DataState<Unit>
    suspend fun reportPost(postId: String, reason: String?): DataState<Unit>
    suspend fun sharePost(postId: String): DataState<Unit>
    suspend fun addComment(postId: String, comment: String): DataState<Unit>
    suspend fun createPost(post : CreatePost): DataState<Unit>
    suspend fun deletePost(postId: String):DataState<Unit>
}