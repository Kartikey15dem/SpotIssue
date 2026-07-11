package org.example.project.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.example.project.core.model.home.MediaType
import org.example.project.core.model.home.Post
import org.example.project.core.model.home.PostLevel
import kotlin.time.Clock

@Entity(
    tableName = "posts",
    indices = [
        androidx.room.Index(value = ["postLevel", "createdAt", "id"])
    ]
)
data class PostEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val userUrl: String,
    val userName: String,
    val timeAgo: String,
    val postLevel: String,
    val location: String,
    val locality: String? = null,
    val district: String? = null,
    val state: String? = null,
    val country: String? = null,
    val postText: String,
    val mediaType: String,
    val mediaUrlsStr: String,
    val likes: Int,
    val comments: Int,
    val cachedAt: Long,
    val createdAt: Long,
    val isLiked: Boolean = false,
    val isReported: Boolean = false
)

fun PostEntity.toPost(): Post {
    return Post(
        id = id,
        userId = userId,
        userUrl = userUrl,
        userName = userName,
        timeAgo = timeAgo,
        postLevel = PostLevel.valueOf(postLevel),
        postText = postText,
        mediaType = MediaType.valueOf(mediaType),
        mediaUrls = if (mediaUrlsStr.isBlank()) emptyList() else mediaUrlsStr.split(","),
        likes = likes,
        comments = comments,
        isLiked = isLiked,
        isReported = isReported,
        locality = locality,
        district = district,
        state = state,
        country = country,
        createdAt = createdAt
    )
}

fun Post.toEntity(cachedAt: Long = Clock.System.now().toEpochMilliseconds()): PostEntity {
    return PostEntity(
        id = id,
        userId = userId,
        userUrl = userUrl,
        userName = userName,
        timeAgo = timeAgo,
        postLevel = postLevel.name,
        postText = postText,
        mediaType = mediaType.name,
        mediaUrlsStr = mediaUrls?.joinToString(",") ?: "",
        likes = likes,
        comments = comments,
        cachedAt = cachedAt,
        location = locality ?: "", // Deprecated, keeping for fallback
        locality = locality,
        district = district,
        state = state,
        country = country,
        isLiked = isLiked,
        isReported = isReported,
        createdAt = createdAt
    )
}
