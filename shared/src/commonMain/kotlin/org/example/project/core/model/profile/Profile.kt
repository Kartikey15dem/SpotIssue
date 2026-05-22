package org.example.project.core.model.profile

import org.example.project.core.model.home.Post

data class Profile(
    val imageUrl: String,
    val name: String,
    val location: String, // Full formatted location
    val locality: String = "",
    val district: String = "",
    val state: String = "",
    val country: String = "",
    val totalPosts: Int,
    val acks: Int,
    val postByArea: List<Int>,
    val myPosts: List<Post>,
    val ackPosts: List<Post>,
)

