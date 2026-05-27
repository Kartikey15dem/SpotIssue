package org.example.project.core.data.repositoryImp

import org.example.project.core.data.repository.PostRepository
import org.example.project.core.network.services.PostService
import org.example.project.core.model.createPost.CreatePost
import org.example.project.core.datastore.UserPreferencesRepository
import org.example.project.core.network.dto.CoordinatesDto
import org.example.project.core.network.dto.CreatePostRequestDto
import org.example.project.core.network.dto.AddCommentRequestDto
import org.example.project.core.network.dto.ReportPostRequestDto
import org.example.project.core.network.dto.PagedResponse
import androidx.paging.PagingData
import androidx.paging.Pager
import androidx.paging.PagingConfig
import kotlinx.coroutines.flow.Flow
import org.example.project.core.data.paging.CommentPagingSource
import org.example.project.core.network.dto.CommentDto
import org.example.project.core.model.home.Comment
import org.example.project.core.utils.DataState
import org.example.project.core.utils.safeApiCall

class PostRepositoryImpl(
    private val postService: PostService,
) : PostRepository {

    override suspend fun likePost(postId: String): DataState<Unit> = safeApiCall {
        postService.likePost(postId)
    }

    override suspend fun reportPost(postId: String, reason: String?): DataState<Unit> = safeApiCall {
        postService.reportPost(postId, ReportPostRequestDto(reason))
    }

    override suspend fun sharePost(postId: String): DataState<Unit> = safeApiCall {
        postService.sharePost(postId)
    }


    
    
    override suspend fun getComments(postId: String, page: Int, limit: Int): DataState<PagedResponse<CommentDto>> = safeApiCall {
        postService.getComments(postId, page, limit)
    }

    override fun getPagedComments(postId: String): Flow<PagingData<Comment>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = { CommentPagingSource(postService, postId) }
        ).flow
    }


    override suspend fun addComment(postId: String, comment: String): DataState<Unit> = safeApiCall {
        postService.addComment(postId, AddCommentRequestDto(comment))
    }

    override suspend fun createPost(post: CreatePost): DataState<Unit> = safeApiCall {
        val request = CreatePostRequestDto(
            postLevel = post.postLevel.name,
            postText = post.postText,
            mediaType = post.mediaType.name,
            mediaUrl = post.mediaUrl,
            locality = post.location?.locality,
            district = post.location?.district,
            state = post.location?.state,
            country = post.location?.country,
            coordinates = post.location?.latitude?.let { lat ->
                post.location?.longitude?.let { lon ->
                    CoordinatesDto(lat, lon)
                }
            }
        )
        postService.createPost(request)
        Unit
    }

    override suspend fun deletePost(postId: String): DataState<Unit> = safeApiCall {
        postService.deletePost(postId)
    }
}
