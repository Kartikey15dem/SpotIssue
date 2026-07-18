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
import org.example.project.core.utils.getRelativeTime
import org.example.project.core.utils.parseIsoEpochMillis
import kotlin.time.Clock

fun PostWithProfileDto.toPost(): Post {
    val parsedCreatedAt = parseIsoEpochMillis(createdAt)

    return Post(
        id = id,
        userId = userId,
        userName = profile?.name ?: "Unknown",
        userUrl = profile?.imageUrl ?: "",
        postText = postText,
        mediaType = when (mediaType) {
            "IMAGE" -> MediaType.IMAGE
            "VIDEO" -> MediaType.VIDEO
            "PDF" -> MediaType.PDF
            else -> MediaType.IMAGE
        },
        mediaUrls = mediaUrls,
        postLevel = try { PostLevel.valueOf(postLevel) } catch(e: Exception) { PostLevel.LOCALITY },
        likes = likes,
        comments = comments,
        timeAgo = createdAt?.let { getRelativeTime(it) } ?: "Just now",
        locality = locality,
        district = district,
        state = state,
        country = country,
        coordinates = coordinates?.let { Coordinates(it.latitude, it.longitude) },
        isLiked = isLiked,
        isReported = isReported,
        createdAt = parsedCreatedAt,
        cachedAt = parsedCreatedAt
    )
}

fun ProfileDto.toEntity(userId: String = "current_user"): ProfileEntity {
    return ProfileEntity(
        userId = userId,
        name = name,
        email = email,
        imageUrl = imageUrl,
        totalPosts = totalPosts,
        acks = acks,
        postByAreaStr = postByArea?.joinToString(","),
        lastSyncedAt = Clock.System.now().toEpochMilliseconds(),
    )
}

fun Post.toUserPostEntity(cachedAt: Long = 0L): UserPostEntity {
    val now = Clock.System.now().toEpochMilliseconds()
    return UserPostEntity(
        id = id,
        
        userId = userId,
        userName = userName,
        userAvatar = userUrl,
        postText = postText,
        mediaType = mediaType.name,
        mediaUrlsStr = mediaUrls?.joinToString(",") ?: "",
        location = locality ?: "",
        locality = locality,
        district = district,
        state = state,
        country = country,
        postLevel = postLevel.name,
        likes = likes,
        comments = comments,
        isLiked = isLiked,
        isReported = isReported,
        timeAgo = timeAgo,
        createdAt = createdAt,
        cachedAt = if (cachedAt == 0L) now else cachedAt
    )
}

fun Post.toLikedPostEntity(cachedAt: Long = 0L): LikedPostEntity {
    val now = Clock.System.now().toEpochMilliseconds()
    return LikedPostEntity(
        id = id,
        
        userId = userId,
        userName = userName,
        userAvatar = userUrl,
        postText = postText,
        mediaType = mediaType.name,
        mediaUrlsStr = mediaUrls?.joinToString(",") ?: "",
        location = locality ?: "",
        locality = locality,
        district = district,
        state = state,
        country = country,
        postLevel = postLevel.name,
        likes = likes,
        comments = comments,
        isLiked = isLiked,
        isReported = isReported,
        timeAgo = timeAgo,
        createdAt = createdAt,
        likedAt = now,
        cachedAt = if (cachedAt == 0L) now else cachedAt
    )
}
