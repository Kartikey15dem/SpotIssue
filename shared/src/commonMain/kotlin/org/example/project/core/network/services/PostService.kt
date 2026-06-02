package org.example.project.core.network.services

import de.jensklingenberg.ktorfit.http.Body
import de.jensklingenberg.ktorfit.http.DELETE
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Multipart
import de.jensklingenberg.ktorfit.http.Part
import io.ktor.client.request.forms.MultiPartFormDataContent
import de.jensklingenberg.ktorfit.http.POST
import de.jensklingenberg.ktorfit.http.Path
import de.jensklingenberg.ktorfit.http.Query
import org.example.project.core.utils.ApiEndPoints
import org.example.project.core.network.dto.CreatePostRequestDto
import org.example.project.core.network.dto.*
import org.example.project.core.network.dto.PostWithProfileDto

interface PostService {
    @POST(ApiEndPoints.POSTS + "/{postid}/like")
    suspend fun likePost(@Path("postid") id: String)

    @POST(ApiEndPoints.POSTS + "/{postid}/report")
    suspend fun reportPost(@Path("postid") id: String, @Body request: ReportPostRequestDto)

    @POST(ApiEndPoints.POSTS + "/{postid}/comments")
    suspend fun addComment(@Path("postid") id: String, @Body request: AddCommentRequestDto)

    @POST(ApiEndPoints.POSTS + "/{id}/share")
    suspend fun sharePost(@Path("id") id: String)

    @GET(ApiEndPoints.POSTS + "/{id}/comments")
    suspend fun getComments(
        @Path("id") id: String,
        @Query("page") page: Int,
        @Query("limit") limit: Int
    ): PagedResponse<CommentDto>

    @Multipart
    @POST(ApiEndPoints.POSTS)
    suspend fun createPost(@Body body: MultiPartFormDataContent)

    @DELETE(ApiEndPoints.POSTS + "/{postid}")
    suspend fun deletePost(@Path("postid") id: String)
}
