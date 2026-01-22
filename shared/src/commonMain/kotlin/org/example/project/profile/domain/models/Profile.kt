package org.example.project.profile.domain.models

import org.example.project.home.domain.models.Post

data class Profile(
    val imageUrl: String,
    val name: String,
    val location: String, // Full formatted location
    val locality: String = "", // e.g., "Downtown"
    val district: String = "", // e.g., "Mumbai Central"
    val state: String = "", // e.g., "Maharashtra"
    val country: String = "", // e.g., "India"
    val totalPosts: Int,
    val acks: Int,
    val postByArea: List<Int>,
    val myPosts: List<Post>,
    val ackPosts: List<Post>,
)