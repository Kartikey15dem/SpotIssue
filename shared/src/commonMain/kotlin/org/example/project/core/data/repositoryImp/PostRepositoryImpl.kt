package org.example.project.core.data.repositoryImp

import org.example.project.core.data.repository.PostRepository
import org.example.project.core.network.services.PostService
import org.example.project.core.model.createPost.CreatePost
import org.example.project.core.network.dto.CoordinatesDto
import org.example.project.core.network.dto.CreatePostRequestDto
import org.example.project.core.network.dto.AddCommentRequestDto
import org.example.project.core.network.dto.ReportPostRequestDto
import org.example.project.core.model.home.Post
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import org.example.project.core.network.dto.PagedResponse
import androidx.paging.PagingData
import androidx.paging.Pager
import androidx.paging.PagingConfig
import io.ktor.utils.io.core.build
import kotlinx.coroutines.flow.Flow
import okio.SYSTEM
import org.example.project.core.network.dto.CommentDto
import org.example.project.core.model.home.Comment
import org.example.project.core.utils.DataState
import org.example.project.core.utils.safeApiCall
import org.example.project.core.database.IssueSpotDatabase
import co.touchlab.kermit.Logger
import org.example.project.core.database.entities.toEntity
import org.example.project.core.data.mappers.toPost
import org.example.project.core.data.mappers.toUserPostEntity
import org.example.project.core.network.NetworkMonitor

class PostRepositoryImpl(
    private val postService: PostService,
    private val database: IssueSpotDatabase,
    private val networkMonitor: NetworkMonitor,
) : PostRepository {

    private val logger = Logger.withTag("PostRepository")

    override suspend fun likePost(postId: String): DataState<Unit> {
        logger.d { "Liking post: $postId" }
        val result = safeApiCall(networkMonitor) {
            postService.likePost(postId)
        }
        if (result is DataState.Success) {
            // Update local DB instantly
            val isLikedCurrent = database.postDao().getPostById(postId)?.isLiked
                ?: database.userPostDao().getPostById(postId)?.isLiked
                ?: database.likedPostDao().getLikedPostById(postId)?.isLiked
                ?: false
                
            val likesCountCurrent = database.postDao().getPostById(postId)?.likes
                ?: database.userPostDao().getPostById(postId)?.likes
                ?: database.likedPostDao().getLikedPostById(postId)?.likes
                ?: 0

            val newIsLiked = !isLikedCurrent
            val newLikesCount = if (newIsLiked) likesCountCurrent + 1 else (likesCountCurrent - 1).coerceAtLeast(0)
            
            database.postDao().updateLikeStatus(postId, newLikesCount, newIsLiked)
            database.userPostDao().updatePostLikeStatus(postId, newLikesCount, newIsLiked)
            database.likedPostDao().updatePostLikeStatus(postId, newLikesCount, newIsLiked)
            
            // Sync with fresh data from API
            safeApiCall(networkMonitor) { postService.getPost(postId) }.let { freshResult ->
                if (freshResult is DataState.Success) {
                    val entity = freshResult.data.toPost().toEntity()
                    database.postDao().insertPosts(listOf(entity))
                }
            }
        } else {
            logger.e { "Failed to like post: $postId" }
        }
        return result
    }

    override suspend fun reportPost(postId: String, reason: String?): DataState<Unit> {
        logger.d { "Reporting post: $postId, reason: $reason" }
        val result = safeApiCall(networkMonitor) {
            postService.reportPost(postId, ReportPostRequestDto(reason))
        }
        if (result is DataState.Success) {
            database.postDao().updateReportStatus(postId, true)
            database.userPostDao().updateReportStatus(postId, true)
            database.likedPostDao().updateReportStatus(postId, true)
        } else {
            logger.e { "Failed to report post: $postId" }
        }
        return result
    }

    override suspend fun sharePost(postId: String): DataState<Unit> {
        logger.d { "Sharing post: $postId" }
        return safeApiCall(networkMonitor) {
            postService.sharePost(postId)
        }
    }

    override suspend fun getComments(postId: String, page: Int, limit: Int): DataState<PagedResponse<CommentDto>> = safeApiCall(networkMonitor) {
        postService.getComments(postId, page, limit)
    }

    override fun getPagedComments(postId: String): Flow<PagingData<Comment>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = { org.example.project.core.data.paging.CommentPagingSource(postService, postId, networkMonitor) }
        ).flow
    }

    override suspend fun addComment(postId: String, comment: String): DataState<Unit> {
        logger.d { "Adding comment to post: $postId" }
        val result = safeApiCall(networkMonitor) {
            postService.addComment(postId, AddCommentRequestDto(comment))
        }
        if (result is DataState.Success) {
            val commentsCountCurrent = database.postDao().getPostById(postId)?.comments
                ?: database.userPostDao().getPostById(postId)?.comments
                ?: database.likedPostDao().getLikedPostById(postId)?.comments
                ?: 0

            val newCommentsCount = commentsCountCurrent + 1
            database.postDao().updateCommentsCount(postId, newCommentsCount)
            database.userPostDao().updateCommentsCount(postId, newCommentsCount)
            database.likedPostDao().updateCommentsCount(postId, newCommentsCount)
            
            
            safeApiCall(networkMonitor) { postService.getPost(postId) }.let { freshResult ->
                if (freshResult is DataState.Success) {
                    database.postDao().insertPosts(listOf(freshResult.data.toPost().toEntity()))
                }
            }
        } else {
            logger.e { "Failed to add comment to post: $postId" }
        }
        return result
    }

    override suspend fun createPost(post: org.example.project.core.model.createPost.CreatePost): DataState<Post> {
        logger.d { "Creating post" }
        val request = CreatePostRequestDto(
            postText = post.postText,
            mediaType = post.mediaType?.name,
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
        
        val multipartParts = mutableListOf<io.ktor.http.content.PartData>()
        
        multipartParts.add(
            io.ktor.http.content.PartData.FormItem(
                value = Json.encodeToString(request),
                dispose = {},
                partHeaders = Headers.build {
                    append(HttpHeaders.ContentDisposition, "form-data; name=\"request\"")
                    append(HttpHeaders.ContentType, "application/json")
                }
            )
        )
        
        post.mediaFilePaths.forEach { filePath ->
            val path = filePath.toPath()
            val fileName = path.name
            val bytes = FileSystem.SYSTEM.source(path).buffer().readByteArray()
            
            multipartParts.add(
                io.ktor.http.content.PartData.FileItem(
                    provider = { io.ktor.utils.io.ByteReadChannel(bytes) },
                    dispose = {},
                    partHeaders = Headers.build {
                        append(HttpHeaders.ContentDisposition, "form-data; name=\"files\"; filename=\"$fileName\"")
                        append(HttpHeaders.ContentType, "application/octet-stream")
                    }
                )
            )
        }
        
        val result = safeApiCall(networkMonitor) {
            val dto = postService.createPost(MultiPartFormDataContent(multipartParts))
            dto.toPost()
        }

        if (result is DataState.Success) {
            val createdPost = result.data
            // 1. Add to user posts for instant visibility
            database.userPostDao().insertPosts(listOf(createdPost.toUserPostEntity()))
            
            // 2. Update profile stats locally
            val profile = database.profileDao().getProfile()
            if (profile != null) {
                val newTotalPosts = profile.totalPosts + 1
                val postByArea = profile.postByAreaStr?.split(",")?.map { it.toIntOrNull() ?: 0 }?.toMutableList() ?: mutableListOf(0, 0, 0, 0)
                if (postByArea.size >= 4) {
                    postByArea[0] = postByArea[0] + 1 // Add to LOCALITY level
                }
                val newPostByAreaStr = postByArea.joinToString(",")
                
                database.profileDao().upsertProfile(profile.copy(
                    totalPosts = newTotalPosts,
                    postByAreaStr = newPostByAreaStr
                ))
            }
        }
        
        return result
    }

    override suspend fun deletePost(postId: String): DataState<Unit> {
        logger.d { "Deleting post: $postId" }
        // Delete locally first for instant UI feedback
        database.postDao().deletePostById(postId)
        database.userPostDao().deletePost(postId)
        database.likedPostDao().deleteLikedPost(postId)
        
        val result = safeApiCall(networkMonitor) {
            postService.deletePost(postId)
        }
        if (result !is DataState.Success) {
            logger.e { "Failed to delete post: $postId" }
        }
        return result
    }

    override suspend fun getPost(postId: String): DataState<Post> = safeApiCall(networkMonitor) {
        val dto = postService.getPost(postId)
        dto.toPost()
    }
}
