package org.example.project.core.data.mappers

import org.example.project.core.database.entities.LikedPostEntity
import org.example.project.core.database.entities.UserPostEntity
import org.example.project.core.model.home.MediaType
import org.example.project.core.model.home.Post
import org.example.project.core.model.home.PostLevel
import org.example.project.core.network.dto.PostWithProfileDto

import kotlin.time.Clock

fun PostWithProfileDto.toPost(): Post {
    return Post(
        id = id,
        userName = profile?.name ?: "Unknown",
        userUrl = profile?.imageUrl ?: "",
        postText = postText,
        mediaType = when (mediaType) {
            "IMAGE" -> MediaType.IMAGE
            "VIDEO" -> MediaType.VIDEO
            else -> MediaType.IMAGE
        },
        mediaUrl = mediaUrl,
        location = location,
        postLevel = PostLevel.valueOf(postLevel),
        likes = likes,
        comments = comments,
        timeAgo = "1 hour ago" // Default
    )
}

fun Post.toUserPostEntity(cachedAt: Long = Clock.System.now().toEpochMilliseconds()): UserPostEntity {
    return UserPostEntity(
        id = id,
        userId = "current_user",
        userName = userName,
        userAvatar = userUrl,
        postText = postText,
        mediaType = mediaType.name,
        mediaUrl = mediaUrl,
        location = location,
        postLevel = postLevel.name,
        likes = likes,
        comments = comments,
        isLiked = false,
        timeAgo = timeAgo,
        createdAt = Clock.System.now().toEpochMilliseconds(),
        cachedAt = cachedAt
    )
}

fun Post.toLikedPostEntity(cachedAt: Long = Clock.System.now().toEpochMilliseconds()): LikedPostEntity {
    return LikedPostEntity(
        id = id,
        userId = "current_user",
        userName = userName,
        userAvatar = userUrl,
        postText = postText,
        mediaType = mediaType.name,
        mediaUrl = mediaUrl,
        location = location,
        postLevel = postLevel.name,
        likes = likes,
        comments = comments,
        isLiked = true,
        timeAgo = timeAgo,
        createdAt = Clock.System.now().toEpochMilliseconds(),
        likedAt = Clock.System.now().toEpochMilliseconds(),
        cachedAt = cachedAt
    )
}
