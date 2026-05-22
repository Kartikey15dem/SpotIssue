package org.example.project.core.model.createPost

import org.example.project.core.model.home.MediaType

data class CreatePost(
    val userId: String,
    val userName: String,
    val userUrl: String?,
    val postText: String,
    val mediaType: MediaType,
    val mediaUrl: String?
)