package org.example.project.core.model.home

import kotlinx.serialization.Serializable

data class Coordinates(
    val latitude: Double,
    val longitude: Double
)

data class Post(
    val id: String,
    val userId: String,
    val userUrl: String,
    val userName: String,
    val timeAgo: String,
    val postLevel: PostLevel,
    val postText: String,
    val mediaType: MediaType,
    val mediaUrls: List<String>? = null,
    val likes: Int,
    val comments: Int,
    val locality: String? = null,
    val district: String? = null,
    val state: String? = null,
    val country: String? = null,
    val coordinates: Coordinates? = null,
    val isLiked: Boolean = false,
    val isReported: Boolean = false,
    val createdAt: Long = 0L,
    val cachedAt: Long = 0L
)
@Serializable
enum class MediaType {
    IMAGE,
    VIDEO,
    PDF
}
@Serializable
enum class PostLevel(val displayName: String) {
    LOCALITY("Locality"),
    DISTRICT("District"),
    STATE("State"),
    NATIONAL("National")
}

fun PostLevel.getText(): String {
    return when (this) {
        PostLevel.LOCALITY -> "Issues in your immediate area"
        PostLevel.DISTRICT -> "District-wide concerns and problems"
        PostLevel.STATE -> "State-level issues affecting your region"
        PostLevel.NATIONAL -> "Nationwide issues and concerns"
    }
}

@Serializable
data class SelectedMediaItem(
    val uri: String,
    val type: MediaType
)
