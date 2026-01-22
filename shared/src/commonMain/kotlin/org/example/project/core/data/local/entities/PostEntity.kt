package org.example.project.core.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.example.project.home.domain.models.MediaType
import org.example.project.home.domain.models.Post
import org.example.project.home.domain.models.PostLevel
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
    val mediaUrl: String,
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
        location = location,
        postText = postText,
        mediaType = MediaType.valueOf(mediaType),
        mediaUrl = mediaUrl,
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
        location = location,
        postText = postText,
        mediaType = mediaType.name,
        mediaUrl = mediaUrl,
        likes = likes,
        comments = comments,
        cachedAt = cachedAt
    )
}

