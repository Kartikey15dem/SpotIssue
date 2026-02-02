package org.example.project.home.domain.models


data class Post(
    val id:String,
    val userUrl: String,
    val userName: String,
    val timeAgo: String,
    val postLevel: PostLevel,
    val location: String,
    val postText: String,
    val mediaType: MediaType,
    val mediaUrl: String,
    val likes: Int,
    val comments: Int,
)
enum class MediaType {
    IMAGE,
    VIDEO,
    PDF
}
enum class PostLevel(val displayName: String){
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