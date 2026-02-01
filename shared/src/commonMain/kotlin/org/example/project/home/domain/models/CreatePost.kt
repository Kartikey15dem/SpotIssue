package org.example.project.home.domain.models

data class CreatePost(
    val userId: String,
    val userName: String,
    val userUrl: String?,
    val postText: String,
    val mediaType: MediaType,
    val mediaUrl: String?
)