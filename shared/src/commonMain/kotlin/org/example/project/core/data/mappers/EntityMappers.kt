package org.example.project.core.data.mappers

import org.example.project.core.database.entities.LikedPostEntity
import org.example.project.core.database.entities.UserPostEntity
import org.example.project.core.model.home.MediaType
import org.example.project.core.model.home.Post
import org.example.project.core.model.home.PostLevel
import org.example.project.core.database.entities.ProfileEntity
import org.example.project.core.model.home.Coordinates
import org.example.project.core.network.dto.PostWithProfileDto
import org.example.project.core.network.dto.ProfileDto
import org.example.project.core.network.dto.CoordinatesDto

fun PostWithProfileDto.toPost(): Post {
    return Post(
        id = id,
        userName = profile?.name ?: "Unknown",
        userUrl = profile?.imageUrl ?: "",
        postText = postText,
        mediaType = when (mediaType) {
            "IMAGE" -> MediaType.IMAGE
            "VIDEO" -> MediaType.VIDEO
            "PDF" -> MediaType.PDF
            else -> MediaType.IMAGE
        },
        mediaUrl = mediaUrl ?: "",
        postLevel = PostLevel.valueOf(postLevel),
        likes = likes,
        comments = comments,
        timeAgo = "1 hour ago", // Default
        locality = locality,
        district = district,
        state = state,
        country = country,
        coordinates = coordinates?.let { Coordinates(it.latitude, it.longitude) }
    )
}

fun ProfileDto.toEntity(): ProfileEntity {
    return ProfileEntity(
        userId = id,
        name = name,
        email = email,
        imageUrl = imageUrl,
        totalPosts = totalPosts,
        acks = acks,
        lastSyncedAt = kotlin.time.Clock.System.now().toEpochMilliseconds(),
    )
}

fun Post.toUserPostEntity(
    userId: String = "current_user",
    cachedAt: Long = kotlin.time.Clock.System.now().toEpochMilliseconds()
): UserPostEntity {
    return UserPostEntity(
        id = id,
        userId = userId,
        userName = userName,
        userAvatar = userUrl,
        postText = postText,
        mediaType = mediaType.name,
        mediaUrl = mediaUrl,
        location = locality ?: "", // Use locality as location for now if needed by entity
        postLevel = postLevel.name,
        likes = likes,
        comments = comments,
        isLiked = false,
        timeAgo = timeAgo,
        createdAt = kotlin.time.Clock.System.now().toEpochMilliseconds(),
        cachedAt = cachedAt
    )
}

fun Post.toLikedPostEntity(
    userId: String = "current_user",
    cachedAt: Long = kotlin.time.Clock.System.now().toEpochMilliseconds()
): LikedPostEntity {
    return LikedPostEntity(
        id = id,
        userId = userId,
        userName = userName,
        userAvatar = userUrl,
        postText = postText,
        mediaType = mediaType.name,
        mediaUrl = mediaUrl,
        location = locality ?: "",
        postLevel = postLevel.name,
        likes = likes,
        comments = comments,
        isLiked = true,
        timeAgo = timeAgo,
        createdAt = kotlin.time.Clock.System.now().toEpochMilliseconds(),
        likedAt = kotlin.time.Clock.System.now().toEpochMilliseconds(),
        cachedAt = cachedAt
    )
}
