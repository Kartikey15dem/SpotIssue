package org.example.project.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.example.project.core.model.home.MediaType
import org.example.project.core.model.home.Post
import org.example.project.core.model.home.PostLevel
import kotlin.time.Clock

/**
 * Room entity for caching posts locally
 */
@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey
    val id: String,
    val userUrl: String,
    val userName: String,
    val timeAgo: String,
    val postLevel: String, // Store as String to avoid Room enum issues
    val location: String,
    val postText: String,
    val mediaType: String, // Store as String
    val mediaUrlsStr: String, // Comma separated list
    val likes: Int,
    val comments: Int,
    val cachedAt: Long // Timestamp when cached
)

/**
 * Convert PostEntity to domain Post model
 */
fun PostEntity.toPost(): Post {
    return Post(
        id = id,
        userUrl = userUrl,
        userName = userName,
        timeAgo = timeAgo,
        postLevel = PostLevel.valueOf(postLevel),
        postText = postText,
        mediaType = MediaType.valueOf(mediaType),
        mediaUrls = if (mediaUrlsStr.isBlank()) emptyList() else mediaUrlsStr.split(","),
        likes = likes,
        comments = comments
    )
}

/**
 * Convert domain Post model to PostEntity
 */
fun Post.toEntity(cachedAt: Long = Clock.System.now().toEpochMilliseconds()): PostEntity {
    return PostEntity(
        id = id,
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
        location =  ""
    )
}

