package org.example.project.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for storing user's own posts locally
 */
@Entity(tableName = "user_posts", primaryKeys = ["id", "sort"])
data class UserPostEntity(
    val id: String,
    val sort: String,
    val userId: String,
    val userName: String,
    val userAvatar: String? = null,
    val postText: String,
    val mediaType: String? = null, // "image", "video", or null
    val mediaUrlsStr: String,
    val location: String,
    val locality: String? = null,
    val district: String? = null,
    val state: String? = null,
    val country: String? = null,
    val postLevel: String, // "LOCALITY", "DISTRICT", "STATE", "COUNTRY"
    val likes: Int = 0,
    val comments: Int = 0,
    val isLiked: Boolean = false,
    val isReported: Boolean = false,
    val timeAgo: String,
    val createdAt: Long, // Timestamp for sorting
    val cachedAt: Long = 0L // Set when inserting
)

