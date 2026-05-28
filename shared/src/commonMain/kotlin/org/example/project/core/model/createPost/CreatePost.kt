package org.example.project.core.model.createPost

import org.example.project.core.model.auth.UserLocation
import org.example.project.core.model.home.MediaType
import org.example.project.core.model.home.PostLevel

data class CreatePost(
    val userId: String,
    val userName: String,
    val userUrl: String?,
    val postLevel: PostLevel,
    val postText: String,
    val mediaType: MediaType,
    val mediaUrls: List<String>?,
    val location: UserLocation? = null
)