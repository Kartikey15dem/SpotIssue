package org.example.project.core.network.services

import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.PUT
import de.jensklingenberg.ktorfit.http.Query
import de.jensklingenberg.ktorfit.http.Body
import org.example.project.core.network.dto.PagedResponse
import org.example.project.core.network.dto.PostWithProfileDto
import org.example.project.core.network.dto.ProfileDto
import org.example.project.core.network.dto.UpsertProfileRequest
import org.example.project.core.utils.ApiEndPoints

import de.jensklingenberg.ktorfit.http.Multipart
import de.jensklingenberg.ktorfit.http.Part
import io.ktor.client.request.forms.MultiPartFormDataContent

import org.example.project.core.network.dto.EmailChangeRequest
import org.example.project.core.network.dto.EmailChangeVerifyRequest
import de.jensklingenberg.ktorfit.http.POST

interface ProfileService {
    @GET(ApiEndPoints.PROFILE + "/me")
    suspend fun getMyProfile(): ProfileDto

    @Multipart
    @PUT(ApiEndPoints.PROFILE + "/me")
    suspend fun updateMyProfile(
        @Body body: MultiPartFormDataContent
    ): ProfileDto

    @POST(ApiEndPoints.PROFILE + "/me/email-change/request")
    suspend fun requestEmailChange(@Body request: EmailChangeRequest)

    @POST(ApiEndPoints.PROFILE + "/me/email-change/verify")
    suspend fun verifyEmailChange(@Body request: EmailChangeVerifyRequest)

    @GET(ApiEndPoints.PROFILE + "/me/posts")
    suspend fun getMyPosts(
        @Query("page") page: Int,
        @Query("limit") limit: Int,
        @Query("sort") sort: String
    ): PagedResponse<PostWithProfileDto>

    @GET(ApiEndPoints.PROFILE + "/me/liked-posts")
    suspend fun getMyLikedPosts(
        @Query("page") page: Int,
        @Query("limit") limit: Int,
        @Query("sort") sort: String
    ): PagedResponse<PostWithProfileDto>
}
