package org.example.project.core.network.services

import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Query
import org.example.project.core.network.dto.PagedResponse
import org.example.project.core.network.dto.ActiveIssuesDto
import org.example.project.core.network.dto.PostWithProfileDto
import org.example.project.core.utils.ApiEndPoints

interface HomeService {
    @GET(ApiEndPoints.HOME + "/posts")
    suspend fun getPosts(
        @Query("level") level: String,
        @Query("page") page: Int,
        @Query("limit") limit: Int
    ): PagedResponse<PostWithProfileDto>

    @GET(ApiEndPoints.HOME + "/active-issues")
    suspend fun getActiveIssuesCount(
        @Query("level") level: String
    ): ActiveIssuesDto
}
