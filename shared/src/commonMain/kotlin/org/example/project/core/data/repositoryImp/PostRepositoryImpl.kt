package org.example.project.core.data.repositoryImp

import org.example.project.core.data.repository.PostRepository
import org.example.project.core.network.services.PostService
import org.example.project.core.model.createPost.CreatePost
import org.example.project.core.datastore.UserPreferencesRepository
import org.example.project.core.network.dto.CoordinatesDto
import org.example.project.core.network.dto.CreatePostRequestDto
import org.example.project.core.network.dto.AddCommentRequestDto
import org.example.project.core.network.dto.ReportPostRequestDto
import org.example.project.core.model.home.Post
import org.example.project.core.data.mappers.toPost
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.append
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.utils.io.core.BytePacketBuilder
import io.ktor.utils.io.core.writeFully
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
        
        postService.createPost(MultiPartFormDataContent(multipartParts))
        Unit
    }

    override suspend fun deletePost(postId: String): DataState<Unit> = safeApiCall {
        postService.deletePost(postId)
    }

    override suspend fun getPost(postId: String): DataState<Post> = safeApiCall {
        val dto = postService.getPost(postId)
        dto.toPost()
    }
}
