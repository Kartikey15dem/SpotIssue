package org.example.project.core.network.services

import de.jensklingenberg.ktorfit.http.Body
import de.jensklingenberg.ktorfit.http.DELETE
import de.jensklingenberg.ktorfit.http.POST
import de.jensklingenberg.ktorfit.http.Path
import org.example.project.core.utils.ApiEndPoints
import org.example.project.core.network.dto.CreatePostRequestDto
import org.example.project.core.network.dto.*
import org.example.project.core.network.dto.PostWithProfileDto

interface PostService {
    @POST(ApiEndPoints.POSTS + "/{id}/like")
    suspend fun likePost(@Path("id") id: String)

    @POST(ApiEndPoints.POSTS + "/{id}/report")
    suspend fun reportPost(@Path("id") id: String, @Body request: ReportPostRequestDto)

    @POST(ApiEndPoints.POSTS + "/{id}/comments")
    suspend fun addComment(@Path("id") id: String, @Body request: AddCommentRequestDto)

    @POST(ApiEndPoints.POSTS + "/{id}/share")
    suspend fun sharePost(@Path("id") id: String)

    @POST(ApiEndPoints.POSTS)
    suspend fun createPost(@Body request: CreatePostRequestDto): PostWithProfileDto

    @DELETE(ApiEndPoints.POSTS + "/{id}")
    suspend fun deletePost(@Path("id") id: String)
}
