package org.example.project.core.network.services

import de.jensklingenberg.ktorfit.http.Body
import de.jensklingenberg.ktorfit.http.DELETE
import de.jensklingenberg.ktorfit.http.POST
import de.jensklingenberg.ktorfit.http.Path
import org.example.project.core.utils.ApiEndPoints
import org.example.project.core.model.createPost.CreatePost

interface PostService {
    @POST(ApiEndPoints.HOME + "/posts/{id}/like")
    suspend fun likePost(@Path("id") id: String): Unit

    @POST(ApiEndPoints.HOME + "/posts/{id}/report")
    suspend fun reportPost(@Path("id") id: String, @Body reason: String?): Unit

    @POST(ApiEndPoints.HOME + "/posts/{id}/comment")
    suspend fun addComment(@Path("id") id: String, @Body comment: String): Unit

    @POST(ApiEndPoints.HOME + "/posts")
    suspend fun createPost(@Body post: CreatePost): Unit

    @DELETE(ApiEndPoints.HOME + "/posts/{id}")
    suspend fun deletePost(@Path("id") id: String): Unit
}
