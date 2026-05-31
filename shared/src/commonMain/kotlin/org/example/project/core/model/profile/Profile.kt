package org.example.project.core.model.profile

import org.example.project.core.model.home.Post

data class Profile(
    val imageUrl: String,
    val name: String,
    val email: String,
    val location: String? = null,
    val totalPosts: Int,
    val acks: Int,
    val postByArea: List<Int> = listOf(0, 0, 0, 0),
    val myPosts: List<Post> = emptyList(),
    val ackPosts: List<Post> = emptyList()
)
