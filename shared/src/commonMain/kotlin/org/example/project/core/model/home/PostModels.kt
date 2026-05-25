package org.example.project.core.model.home

data class Coordinates(
    val latitude: Double,
    val longitude: Double
)

data class Post(
    val id: String,
    val userUrl: String,
    val userName: String,
    val timeAgo: String,
    val postLevel: PostLevel,
    val postText: String,
    val mediaType: MediaType,
    val mediaUrl: String,
    val likes: Int,
    val comments: Int,
    val locality: String? = null,
    val district: String? = null,
    val state: String? = null,
    val country: String? = null,
    val coordinates: Coordinates? = null
)

enum class MediaType {
    IMAGE,
    VIDEO,
    PDF,
    GIF
}

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
