package org.example.project.core.data.repositoryImp

import co.touchlab.kermit.Logger
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.content.PartData
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM
import okio.buffer
import org.example.project.core.data.mappers.toPost
import org.example.project.core.data.mappers.toUserPostEntity
import org.example.project.core.data.repository.PostRepository
import org.example.project.core.database.IssueSpotDatabase
import org.example.project.core.database.entities.toPost
import org.example.project.core.model.createPost.CreatePost
import org.example.project.core.model.home.Comment
import org.example.project.core.model.home.Post
import org.example.project.core.model.home.toComment
import org.example.project.core.network.dto.AddCommentRequestDto
import org.example.project.core.network.dto.CommentDto
import org.example.project.core.network.dto.CoordinatesDto
import org.example.project.core.network.dto.CreatePostRequestDto
import org.example.project.core.network.dto.PagedResponse
import org.example.project.core.network.dto.ReportPostRequestDto
import org.example.project.core.network.services.PostService
import org.example.project.core.presentation.FeedError
import org.example.project.core.presentation.PaginationState
import org.example.project.core.utils.DataState
import org.example.project.core.utils.NetworkMonitor
import org.example.project.core.utils.safeApiCall

private const val DB_TRACE = "[DB_TRACE]"

/**
 * PostRepositoryImpl
 *
 * This repository handles single-post actions like Liking, Reporting, Deleting,
 * and fetching/adding Comments.
 *
 * ARCHITECTURE NOTE:
 * - Comments Pagination: Unlike Feed/Profile which use the generic OfflinePager and Room DB,
 *   comments use a lightweight, IN-MEMORY ONLY pagination system (`PaginationState`).
 *   This is because we don't need to permanently cache comments offline for every post.
 * - Optimistic Updates: Actions like Liking and Reporting update the Local Database (Room)
 *   IMMEDIATELY before waiting for the API. If the API fails, we roll back the database change.
 *   This ensures the UI feels lightning fast.
 */
class PostRepositoryImpl(
    private val postService: PostService,
    private val database: IssueSpotDatabase,
    private val networkMonitor: NetworkMonitor,
) : PostRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // In-memory caching for comments. We use a Map because the user can open
    // multiple post comment sections, and we need to track state for each postId independently.
    private val commentsStateMap = mutableMapOf<String, MutableStateFlow<PaginationState<Comment>>>()
    private val commentsNextPageMap = mutableMapOf<String, Int?>()
    private val commentsMutexMap = mutableMapOf<String, Mutex>()

    override fun observeComments(postId: String): StateFlow<PaginationState<Comment>> = getOrPutCommentsState(postId).asStateFlow()

    private fun getOrPutCommentsState(postId: String): MutableStateFlow<PaginationState<Comment>> =
        commentsStateMap.getOrPut(postId) {
            MutableStateFlow(PaginationState())
        }

    private fun getOrPutCommentsMutex(postId: String): Mutex = commentsMutexMap.getOrPut(postId) { Mutex() }

    override fun startComments(postId: String) {
        val state = getOrPutCommentsState(postId)
        if (state.value.items.isEmpty()) {
            refreshComments(postId)
        }
    }

    override fun stopComments(postId: String) {
        // Nothing to do for remote-only paging
    }

    override fun refreshComments(postId: String) {
        scope.launch {
            val state = getOrPutCommentsState(postId)
            val mutex = getOrPutCommentsMutex(postId)

            mutex.withLock {
                state.update { it.copy(isLoading = true, error = null) }
            }

            try {
                val response = postService.getComments(postId, page = 0, limit = 10)
                val newItems = response.items.map { it.toComment() }
                commentsNextPageMap[postId] = response.nextKey
                mutex.withLock {
                    state.update {
                        it.copy(
                            isLoading = false,
                            items = newItems,
                            hasMore = response.nextKey != null && newItems.isNotEmpty(),
                            error = null,
                        )
                    }
                }
            } catch (e: Exception) {
                mutex.withLock {
                    state.update { it.copy(isLoading = false, error = FeedError.Unknown(e.message ?: "Failed to load comments")) }
                }
            }
        }
    }

    override fun loadMoreComments(postId: String) {
        scope.launch {
            val state = getOrPutCommentsState(postId)
            val mutex = getOrPutCommentsMutex(postId)

            var nextPage: Int? = null
            mutex.withLock {
                val current = state.value
                if (current.isLoading || current.isAppending || !current.hasMore) return@launch
                nextPage = commentsNextPageMap[postId]
                state.update { it.copy(isAppending = true, appendError = null) }
            }

            val pageToLoad = nextPage ?: return@launch

            try {
                val response = postService.getComments(postId, page = pageToLoad, limit = 10)
                val newItems = response.items.map { it.toComment() }
                commentsNextPageMap[postId] = response.nextKey
                mutex.withLock {
                    state.update {
                        it.copy(
                            isAppending = false,
                            items = it.items + newItems,
                            hasMore = response.nextKey != null && newItems.isNotEmpty(),
                            appendError = null,
                        )
                    }
                }
            } catch (e: Exception) {
                mutex.withLock {
                    state.update {
                        it.copy(
                            isAppending = false,
                            appendError = FeedError.Unknown(e.message ?: "Failed to load more comments"),
                        )
                    }
                }
            }
        }
    }

    private val logger = Logger.withTag("PostRepository")

    override suspend fun likePost(postId: String): DataState<Unit> {
        logger.d { "Liking post: $postId" }

        // 1. Fetch current status across all possible database tables
        val postEntity = database.postDao().getPostById(postId)
        val userPostEntity = database.userPostDao().getPostById(postId)
        val likedPostEntity = database.likedPostDao().getLikedPostById(postId)

        val isLikedCurrent = postEntity?.isLiked ?: userPostEntity?.isLiked ?: likedPostEntity?.isLiked ?: false
        val likesCountCurrent = postEntity?.likes ?: userPostEntity?.likes ?: likedPostEntity?.likes ?: 0

        val newIsLiked = !isLikedCurrent
        val newLikesCount = if (newIsLiked) likesCountCurrent + 1 else (likesCountCurrent - 1).coerceAtLeast(0)

        // 2. OPTIMISTIC UPDATE: Update database immediately (UI reacts instantly)
        database.postDao().updateLikeStatus(postId, newLikesCount, newIsLiked)
        database.userPostDao().updatePostLikeStatus(postId, newLikesCount, newIsLiked)
        database.likedPostDao().updatePostLikeStatus(postId, newLikesCount, newIsLiked)

        // 3. Make the API Call
        val result =
            safeApiCall(networkMonitor) {
                postService.likePost(postId)
            }

        // 4. ROLLBACK ON FAILURE: If network fails, revert the DB to its original state
        if (result is DataState.Error) {
            logger.e { "Failed to like post: $postId. Rolling back." }
            database.postDao().updateLikeStatus(postId, likesCountCurrent, isLikedCurrent)
            database.userPostDao().updatePostLikeStatus(postId, likesCountCurrent, isLikedCurrent)
            database.likedPostDao().updatePostLikeStatus(postId, likesCountCurrent, isLikedCurrent)
        }

        return result
    }

    override suspend fun reportPost(
        postId: String,
        reason: String?,
    ): DataState<Unit> {
        logger.d { "Reporting post: $postId, reason: $reason" }

        val postEntity = database.postDao().getPostById(postId)
        val userPostEntity = database.userPostDao().getPostById(postId)
        val likedPostEntity = database.likedPostDao().getLikedPostById(postId)

        val isReportedCurrent = postEntity?.isReported ?: userPostEntity?.isReported ?: likedPostEntity?.isReported ?: false

        // Optimistic Update
        database.postDao().updateReportStatus(postId, true)
        database.userPostDao().updateReportStatus(postId, true)
        database.likedPostDao().updateReportStatus(postId, true)

        val result =
            safeApiCall(networkMonitor) {
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

    override suspend fun getComments(
        postId: String,
        page: Int,
        limit: Int,
    ): DataState<PagedResponse<CommentDto>> =
        safeApiCall(networkMonitor) {
            postService.getComments(postId, page, limit)
        }

    override suspend fun addComment(
        postId: String,
        comment: String,
    ): DataState<Unit> {
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

        val result =
            safeApiCall(networkMonitor) {
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
        val request =
            CreatePostRequestDto(
                postText = post.postText,
                mediaType = post.mediaType?.name,
                postLevel = post.postLevel,
                locality = post.location.locality,
                district = post.location.district,
                state = post.location.state,
                country = post.location.country,
                coordinates = CoordinatesDto(post.location.latitude, post.location.longitude),
            )

        val multipartParts = mutableListOf<PartData>()

        multipartParts.add(
            PartData.FormItem(
                value = Json.encodeToString(request),
                dispose = {},
                partHeaders =
                    Headers.build {
                        append(HttpHeaders.ContentDisposition, "form-data; name=\"request\"")
                        append(HttpHeaders.ContentType, "application/json")
                    },
            ),
        )

        post.mediaFilePaths.forEach { filePath ->
            val path = filePath.toPath()
            val fileName = path.name

            val bytes =
                FileSystem.SYSTEM
                    .source(path)
                    .buffer()
                    .readByteArray()

            multipartParts.add(
                PartData.FileItem(
                    provider = { ByteReadChannel(bytes) },
                    dispose = {},
                    partHeaders =
                        Headers.build {
                            append(HttpHeaders.ContentDisposition, "form-data; name=\"files\"; filename=\"$fileName\"")
                            append(HttpHeaders.ContentType, "application/octet-stream")
                        },
                ),
            )
        }

        val result =
            safeApiCall(networkMonitor) {
                val dto = postService.createPost(MultiPartFormDataContent(multipartParts))
                dto.toPost()
            }

        if (result is DataState.Success) {
            val createdPost = result.data
            database.userPostDao().insertPosts(listOf(createdPost.toUserPostEntity()))

            val profile = database.profileDao().getProfile()
            if (profile != null) {
                val newTotalPosts = profile.totalPosts + 1
                val postByArea =
                    profile.postByAreaStr
                        ?.split(",")
                        ?.map { it.toIntOrNull() ?: 0 }
                        ?.toMutableList() ?: mutableListOf(0, 0, 0, 0)
                if (postByArea.size >= 4) {
                    postByArea[0] = postByArea[0] + 1
                }
                val newPostByAreaStr = postByArea.joinToString(",")

                database.profileDao().upsertProfile(
                    profile.copy(
                        totalPosts = newTotalPosts,
                        postByAreaStr = newPostByAreaStr,
                    ),
                )
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

        val result =
            safeApiCall(networkMonitor) {
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

    override suspend fun getPost(postId: String): DataState<Post> =
        safeApiCall(networkMonitor) {
            val dto = postService.getPost(postId)
            dto.toPost()
        }

    override fun observePost(postId: String): Flow<Post?> = database.postDao().observePost(postId).map { it?.toPost() }
}
