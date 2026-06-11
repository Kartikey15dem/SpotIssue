package org.example.project.core.data.mappers

import org.example.project.core.database.entities.UserPostEntity
import org.example.project.core.database.entities.LikedPostEntity
import org.example.project.core.model.home.MediaType
import org.example.project.core.model.home.Post
import org.example.project.core.model.home.PostLevel
import kotlinx.datetime.Clock

/**
 * Map UserPostEntity to Post domain model
 */
fun UserPostEntity.toPost(): Post = Post(
    id = id,
    userId = userId,
    userName = userName,
    userUrl = userAvatar ?: "",
    postText = postText,
    mediaType = when (mediaType) {
        "IMAGE" -> MediaType.IMAGE
        "VIDEO" -> MediaType.VIDEO
        "PDF" -> MediaType.PDF
        else -> MediaType.IMAGE
    },
    mediaUrls = if (mediaUrlsStr.isBlank()) emptyList() else mediaUrlsStr.split(","),
    postLevel = when (postLevel) {
        "LOCALITY" -> PostLevel.LOCALITY
        "DISTRICT" -> PostLevel.DISTRICT
        "STATE" -> PostLevel.STATE
        "NATIONAL" -> PostLevel.NATIONAL
        else -> PostLevel.LOCALITY
    },
    likes = likes,
    comments = comments,
    isLiked = isLiked,
    isReported = isReported,
    timeAgo = timeAgo,
    createdAt = createdAt,
    locality = locality,
    district = district,
    state = state,
    country = country
)

/**
 * Map LikedPostEntity to Post domain model
 */
fun LikedPostEntity.toPost(): Post = Post(
    id = id,
    userId = userId,
    userName = userName,
    userUrl = userAvatar ?: "",
    postText = postText,
    mediaType = when (mediaType) {
        "IMAGE" -> MediaType.IMAGE
        "VIDEO" -> MediaType.VIDEO
        "PDF" -> MediaType.PDF
        else -> MediaType.IMAGE
    },
    mediaUrls = if (mediaUrlsStr.isBlank()) emptyList() else mediaUrlsStr.split(","),
    postLevel = when (postLevel) {
        "LOCALITY" -> PostLevel.LOCALITY
        "DISTRICT" -> PostLevel.DISTRICT
        "STATE" -> PostLevel.STATE
        "NATIONAL" -> PostLevel.NATIONAL
        else -> PostLevel.LOCALITY
    },
    likes = likes,
    comments = comments,
    isLiked = isLiked,
    isReported = isReported,
    timeAgo = timeAgo,
    createdAt = createdAt,
    locality = locality,
    district = district,
    state = state,
    country = country
)

enum class Sort {
    LATEST, OLDEST, POPULAR
}
