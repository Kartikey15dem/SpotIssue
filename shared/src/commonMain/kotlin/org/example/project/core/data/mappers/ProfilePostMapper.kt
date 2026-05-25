package org.example.project.core.data.mappers

import org.example.project.core.database.entities.UserPostEntity
import org.example.project.core.database.entities.LikedPostEntity
import org.example.project.core.model.home.MediaType
import org.example.project.core.model.home.Post
import org.example.project.core.model.home.PostLevel

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
        else -> MediaType.IMAGE
    },
    mediaUrl = mediaUrl ?: "",
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
        else -> MediaType.IMAGE
    },
    mediaUrl = mediaUrl ?: "",
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
    cachedAt = Clock.System.now().toEpochMilliseconds(),
    location = ""
)

enum class Sort {
    LATEST, OLDEST, POPULAR
}

