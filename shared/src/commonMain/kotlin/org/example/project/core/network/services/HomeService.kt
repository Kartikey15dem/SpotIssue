package org.example.project.core.network.services

import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Query
import org.example.project.core.network.dto.PagedResponse
import org.example.project.core.network.dto.ActiveIssuesDto
import org.example.project.core.network.dto.PostWithProfileDto
import org.example.project.core.utils.ApiEndPoints

interface HomeService {
    // The network call to fetch posts.
    // 'page' parameter represents which page we want to fetch (starting from 0).
    // 'limit' is the maximum number of items requested per page (e.g. 20).
    // Returns a PagedResponse which tells us the next page key.
    @GET(ApiEndPoints.POSTS)
    suspend fun getPosts(
        @Query("level") level: String,
        @Query("locality") locality: String? = null,
        @Query("district") district: String? = null,
        @Query("state") state: String? = null,
        @Query("country") country: String? = null,
        @Query("lat") lat: Double? = null,
        @Query("lon") lon: Double? = null,
        @Query("page") page: Int,
        @Query("limit") limit: Int
    ): PagedResponse<PostWithProfileDto>

    @GET(ApiEndPoints.ACTIVE_ISSUES)
    suspend fun getActiveIssuesCount(
        @Query("level") level: String
    ): ActiveIssuesDto
    // Similar to getPosts, this is a paginated search API.
    // Relies on 'page' and 'limit' to return chunks of search results.
    @GET(ApiEndPoints.POSTS + "/search")
    suspend fun searchPosts(
        @Query("query") query: String,
        @Query("level") level: String,
        @Query("page") page: Int,
        @Query("limit") limit: Int
    ): PagedResponse<PostWithProfileDto>
}
