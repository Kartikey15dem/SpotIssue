package org.example.project.core.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.example.project.core.model.createPost.CreatePost
import org.example.project.core.model.home.Comment
import org.example.project.core.model.home.Post
import org.example.project.core.network.dto.CommentDto
import org.example.project.core.network.dto.PagedResponse
import org.example.project.core.presentation.PaginationState
import org.example.project.core.utils.DataState

interface PostRepository {
    suspend fun likePost(postId: String): DataState<Unit>

    suspend fun reportPost(
        postId: String,
        reason: String?,
    ): DataState<Unit>

    suspend fun sharePost(postId: String): DataState<Unit>

    suspend fun getComments(
        postId: String,
        page: Int,
        limit: Int,
    ): DataState<PagedResponse<CommentDto>>

    fun observeComments(postId: String): StateFlow<PaginationState<Comment>>

    fun startComments(postId: String)

    fun stopComments(postId: String)

    fun refreshComments(postId: String)

    fun loadMoreComments(postId: String)

    suspend fun addComment(
        postId: String,
        comment: String,
    ): DataState<Unit>

    suspend fun createPost(post: CreatePost): DataState<Post>

    suspend fun deletePost(postId: String): DataState<Unit>

    suspend fun getPost(postId: String): DataState<Post>

    fun observePost(postId: String): Flow<Post?>
}
