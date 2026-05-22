package org.example.project.core.network.services

import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Query
import org.example.project.core.network.dto.PagedResponse
import org.example.project.core.network.dto.PostWithProfileDto
import org.example.project.core.utils.ApiEndPoints

interface ProfileService {
    @GET(ApiEndPoints.PROFILE + "/posts")
    suspend fun getUserPosts(
        @Query("userId") userId: String,
        @Query("page") page: Int,
        @Query("limit") limit: Int
    ): PagedResponse<PostWithProfileDto>

    @GET(ApiEndPoints.PROFILE + "/liked-posts")
    suspend fun getLikedPosts(
        @Query("userId") userId: String,
        @Query("page") page: Int,
        @Query("limit") limit: Int
    ): PagedResponse<PostWithProfileDto>
}
