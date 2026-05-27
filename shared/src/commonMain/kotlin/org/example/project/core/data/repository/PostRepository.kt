package org.example.project.core.data.repository

import org.example.project.core.model.createPost.CreatePost
import org.example.project.core.network.dto.CommentDto
import org.example.project.core.network.dto.PagedResponse
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import org.example.project.core.model.home.Comment
import org.example.project.core.utils.DataState

interface PostRepository {
    suspend fun likePost(postId: String): DataState<Unit>
    suspend fun reportPost(postId: String, reason: String?): DataState<Unit>
    suspend fun sharePost(postId: String): DataState<Unit>
    fun getPagedComments(postId: String): Flow<PagingData<Comment>>
    suspend fun getComments(postId: String, page: Int, limit: Int): DataState<PagedResponse<CommentDto>>
    suspend fun addComment(postId: String, comment: String): DataState<Unit>
    suspend fun createPost(post : CreatePost): DataState<Unit>
    suspend fun deletePost(postId: String):DataState<Unit>
}