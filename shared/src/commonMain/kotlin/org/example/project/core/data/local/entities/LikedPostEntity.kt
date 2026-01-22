package org.example.project.core.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for storing user's liked posts locally
 */
@Entity(tableName = "liked_posts")
data class LikedPostEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val userName: String,
    val userAvatar: String? = null,
    val postText: String,
    val mediaType: String? = null, // "image", "video", or null
    val mediaUrl: String? = null,
    val location: String,
    val locality: String? = null,
    val district: String? = null,
    val state: String? = null,
    val postLevel: String, // "LOCALITY", "DISTRICT", "STATE", "COUNTRY"
    val likes: Int = 0,
    val comments: Int = 0,
    val isLiked: Boolean = true, // Always true for liked posts
    val timeAgo: String,
    val createdAt: Long, // Timestamp for sorting
    val likedAt: Long = 0L, // When user liked it (set when inserting)
    val cachedAt: Long = 0L // Set when inserting
)

