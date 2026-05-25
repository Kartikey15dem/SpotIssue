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

interface ProfileService {
    @GET(ApiEndPoints.PROFILE + "/me")
    suspend fun getMyProfile(): ProfileDto

    @PUT(ApiEndPoints.PROFILE + "/me")
    suspend fun updateMyProfile(
        @Body request: UpsertProfileRequest
    ): ProfileDto

    @GET(ApiEndPoints.PROFILE + "/me/posts")
    suspend fun getMyPosts(
        @Query("page") page: Int,
        @Query("limit") limit: Int
    ): PagedResponse<PostWithProfileDto>

    @GET(ApiEndPoints.PROFILE + "/me/liked-posts")
    suspend fun getMyLikedPosts(
        @Query("page") page: Int,
        @Query("limit") limit: Int
    ): PagedResponse<PostWithProfileDto>
}
