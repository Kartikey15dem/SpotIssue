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
import io.ktor.http.content.PartData
import io.ktor.utils.io.ByteReadChannel
import kotlin.time.Clock
import kotlinx.datetime.Instant
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import org.example.project.core.network.dto.PagedResponse
import androidx.paging.PagingData
import androidx.paging.Pager
import androidx.paging.PagingConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okio.SYSTEM
import org.example.project.core.network.dto.CommentDto
import org.example.project.core.model.home.Comment
import org.example.project.core.utils.DataState
import org.example.project.core.utils.safeApiCall
import org.example.project.core.database.IssueSpotDatabase
import co.touchlab.kermit.Logger
import org.example.project.core.database.entities.toEntity
import org.example.project.core.database.entities.toPost
import org.example.project.core.data.mappers.toPost
import org.example.project.core.data.mappers.toUserPostEntity
import org.example.project.core.utils.NetworkMonitor

import org.example.project.core.data.paging.CommentPagingSource
import org.example.project.core.model.home.toComment

class PostRepositoryImpl(
    private val postService: PostService,
    private val database: IssueSpotDatabase,
    private val networkMonitor: NetworkMonitor,
) : PostRepository {

    private val logger = Logger.withTag("PostRepository")

    override suspend fun likePost(postId: String): DataState<Unit> {
        logger.d { "Liking post: $postId" }
        
        val postEntity = database.postDao().getPostById(postId)
        val userPostEntity = database.userPostDao().getPostById(postId)
        val likedPostEntity = database.likedPostDao().getLikedPostById(postId)
        
        val isLikedCurrent = postEntity?.isLiked ?: userPostEntity?.isLiked ?: likedPostEntity?.isLiked ?: false
        val likesCountCurrent = postEntity?.likes ?: userPostEntity?.likes ?: likedPostEntity?.likes ?: 0
        
        val newIsLiked = !isLikedCurrent
        val newLikesCount = if (newIsLiked) likesCountCurrent + 1 else (likesCountCurrent - 1).coerceAtLeast(0)
        
        // Optimistic Update
        database.postDao().updateLikeStatus(postId, newLikesCount, newIsLiked)
        database.userPostDao().updatePostLikeStatus(postId, newLikesCount, newIsLiked)
        database.likedPostDao().updatePostLikeStatus(postId, newLikesCount, newIsLiked)
        
        val result = safeApiCall(networkMonitor) {
            postService.likePost(postId)
        }
        
        if (result is DataState.Error) {
            logger.e { "Failed to like post: $postId. Rolling back." }
            database.postDao().updateLikeStatus(postId, likesCountCurrent, isLikedCurrent)
            database.userPostDao().updatePostLikeStatus(postId, likesCountCurrent, isLikedCurrent)
            database.likedPostDao().updatePostLikeStatus(postId, likesCountCurrent, isLikedCurrent)
        }
        
        return result
    }

    override suspend fun reportPost(postId: String, reason: String?): DataState<Unit> {
        logger.d { "Reporting post: $postId, reason: $reason" }
        
        val postEntity = database.postDao().getPostById(postId)
        val userPostEntity = database.userPostDao().getPostById(postId)
        val likedPostEntity = database.likedPostDao().getLikedPostById(postId)
        
        val isReportedCurrent = postEntity?.isReported ?: userPostEntity?.isReported ?: likedPostEntity?.isReported ?: false
        
        // Optimistic Update
        database.postDao().updateReportStatus(postId, true)
        database.userPostDao().updateReportStatus(postId, true)
        database.likedPostDao().updateReportStatus(postId, true)
        
        val result = safeApiCall(networkMonitor) {
            postService.reportPost(postId, ReportPostRequestDto(reason))
        }
        
        if (result is DataState.Error) {
            logger.e { "Failed to report post: $postId. Rolling back." }
            database.postDao().updateReportStatus(postId, isReportedCurrent)
            database.userPostDao().updateReportStatus(postId, isReportedCurrent)
            database.likedPostDao().updateReportStatus(postId, isReportedCurrent)
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
            config = PagingConfig(pageSize = 20, enablePlaceholders = false),
            pagingSourceFactory = { CommentPagingSource(postService, postId, networkMonitor) }
        ).flow
    }

    override suspend fun getCommentsList(postId: String): DataState<List<Comment>> {
        return when (val result = getComments(postId, page = 1, limit = 100)) {
            is DataState.Success -> DataState.Success(result.data.items.map { it.toComment() })
            is DataState.Error -> DataState.Error(result.exception)
            DataState.Loading -> DataState.Loading
        }
    }

    override suspend fun addComment(postId: String, comment: String): DataState<Unit> {
        logger.d { "Adding comment to post: $postId" }
        
        val postEntity = database.postDao().getPostById(postId)
        val userPostEntity = database.userPostDao().getPostById(postId)
        val likedPostEntity = database.likedPostDao().getLikedPostById(postId)
        
        val commentsCountCurrent = postEntity?.comments ?: userPostEntity?.comments ?: likedPostEntity?.comments ?: 0
        val newCommentsCount = commentsCountCurrent + 1
        
        // Optimistic Update
        database.postDao().updateCommentsCount(postId, newCommentsCount)
        database.userPostDao().updateCommentsCount(postId, newCommentsCount)
        database.likedPostDao().updateCommentsCount(postId, newCommentsCount)
        
        val result = safeApiCall(networkMonitor) {
            postService.addComment(postId, AddCommentRequestDto(comment))
        }
        
        if (result is DataState.Error) {
            logger.e { "Failed to add comment to post: $postId. Rolling back." }
            database.postDao().updateCommentsCount(postId, commentsCountCurrent)
            database.userPostDao().updateCommentsCount(postId, commentsCountCurrent)
            database.likedPostDao().updateCommentsCount(postId, commentsCountCurrent)
        }
        
        return result
    }

    override suspend fun createPost(post: CreatePost): DataState<Post> {
        logger.d { "Creating post" }
        val request = CreatePostRequestDto(
            postText = post.postText,
            mediaType = post.mediaType?.name,
            postLevel = post.postLevel,
            locality = post.location.locality,
            district = post.location.district,
            state = post.location.state,
            country = post.location.country,
            coordinates = CoordinatesDto(post.location.latitude, post.location.longitude)
        )
        
        val multipartParts = mutableListOf<PartData>()
        
        multipartParts.add(
            PartData.FormItem(
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
                PartData.FileItem(
                    provider = { ByteReadChannel(bytes) },
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
            database.userPostDao().insertPosts(listOf(createdPost.toUserPostEntity(sort = "LATEST")))
            
            val profile = database.profileDao().getProfile()
            if (profile != null) {
                val newTotalPosts = profile.totalPosts + 1
                val postByArea = profile.postByAreaStr?.split(",")?.map { it.toIntOrNull() ?: 0 }?.toMutableList() ?: mutableListOf(0, 0, 0, 0)
                if (postByArea.size >= 4) {
                    postByArea[0] = postByArea[0] + 1
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
        
        val postEntity = database.postDao().getPostById(postId)
        val userPostEntity = database.userPostDao().getPostById(postId)
        val likedPostEntity = database.likedPostDao().getLikedPostById(postId)
        
        database.postDao().deletePostById(postId)
        if (userPostEntity != null) database.userPostDao().deletePost(postId)
        if (likedPostEntity != null) database.likedPostDao().deleteLikedPost(postId)
        
        val result = safeApiCall(networkMonitor) {
            postService.deletePost(postId)
        }
        
        if (result is DataState.Error) {
            logger.e { "Failed to delete post: $postId. Rolling back." }
            if (postEntity != null) database.postDao().insertPosts(listOf(postEntity))
            if (userPostEntity != null) database.userPostDao().insertPosts(listOf(userPostEntity))
            if (likedPostEntity != null) database.likedPostDao().insertPosts(listOf(likedPostEntity))
        }
        return result
    }

    override suspend fun getPost(postId: String): DataState<Post> = safeApiCall(networkMonitor) {
        val dto = postService.getPost(postId)
        dto.toPost()
    }

    override fun observePost(postId: String): Flow<Post?> {
        return database.postDao().observePost(postId).map { it?.toPost() }
    }
}
