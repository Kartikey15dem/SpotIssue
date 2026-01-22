package org.example.project.profile.data.local.mapper

import org.example.project.core.data.local.entities.UserPostEntity
import org.example.project.core.data.local.entities.LikedPostEntity
import org.example.project.home.domain.models.MediaType
import org.example.project.home.domain.models.Post
import org.example.project.home.domain.models.PostLevel
import kotlin.time.Clock

/**
 * Map UserPostEntity to Post domain model
 */
fun UserPostEntity.toPost(): Post = Post(
    id = id,
    userName = userName,
    userUrl = userAvatar ?: "",
    postText = postText,
    mediaType = when (mediaType) {
        "IMAGE" -> MediaType.IMAGE
        "VIDEO" -> MediaType.VIDEO
        "GIF" -> MediaType.GIF
        else -> MediaType.IMAGE
    },
    mediaUrl = mediaUrl ?: "",
    location = location,
    postLevel = when (postLevel) {
        "LOCALITY" -> PostLevel.LOCALITY
        "DISTRICT" -> PostLevel.DISTRICT
        "STATE" -> PostLevel.STATE
        "NATIONAL" -> PostLevel.NATIONAL
        else -> PostLevel.LOCALITY
    },
    likes = likes,
    comments = comments,
    timeAgo = timeAgo
)

/**
 * Map Post to UserPostEntity
 */
fun Post.toUserPostEntity(
    userId: String = "current_user",
    createdAt: Long = Clock.System.now().toEpochMilliseconds()
): UserPostEntity = UserPostEntity(
    id = id,
    userId = userId,
    userName = userName,
    userAvatar = userUrl,
    postText = postText,
    mediaType = mediaType.name,
    mediaUrl = mediaUrl,
    location = location,
    locality = null,
    district = null,
    state = null,
    postLevel = postLevel.name,
    likes = likes,
    comments = comments,
    isLiked = false,
    timeAgo = timeAgo,
    createdAt = createdAt,
    cachedAt = Clock.System.now().toEpochMilliseconds()
)

/**
 * Map LikedPostEntity to Post domain model
 */
fun LikedPostEntity.toPost(): Post = Post(
    id = id,
    userName = userName,
    userUrl = userAvatar ?: "",
    postText = postText,
    mediaType = when (mediaType) {
        "IMAGE" -> MediaType.IMAGE
        "VIDEO" -> MediaType.VIDEO
        "GIF" -> MediaType.GIF
        else -> MediaType.IMAGE
    },
    mediaUrl = mediaUrl ?: "",
    location = location,
    postLevel = when (postLevel) {
        "LOCALITY" -> PostLevel.LOCALITY
        "DISTRICT" -> PostLevel.DISTRICT
        "STATE" -> PostLevel.STATE
        "NATIONAL" -> PostLevel.NATIONAL
        else -> PostLevel.LOCALITY
    },
    likes = likes,
    comments = comments,
    timeAgo = timeAgo
)

/**
 * Map Post to LikedPostEntity
 */
fun Post.toLikedPostEntity(
    userId: String = "current_user",
    createdAt: Long = Clock.System.now().toEpochMilliseconds(),
    likedAt: Long = Clock.System.now().toEpochMilliseconds()
): LikedPostEntity = LikedPostEntity(
    id = id,
    userId = userId,
    userName = userName,
    userAvatar = userUrl,
    postText = postText,
    mediaType = mediaType.name,
    mediaUrl = mediaUrl,
    location = location,
    locality = null,
    district = null,
    state = null,
    postLevel = postLevel.name,
    likes = likes,
    comments = comments,
    isLiked = true,
    timeAgo = timeAgo,
    createdAt = createdAt,
    likedAt = likedAt,
    cachedAt = Clock.System.now().toEpochMilliseconds()
)

enum class Sort {
    LATEST, OLDEST, POPULAR
}

