package org.example.project.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.example.project.core.model.profile.Profile

/**
 * Room entity for storing user profile data locally.
 * Uses a fixed primary key to ensure only one profile is cached at a time.
 * When a new profile is saved, it replaces the existing one.
 */
@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = false)
    val id: Int = 1, // Fixed ID to ensure single profile storage

    // User identification
    val userId: String,
    val email: String? = null,

    // Basic profile info
    val name: String,
    val imageUrl: String? = null,

    // Location information
    val location: String? = null, // Full formatted location
    val locality: String? = null,
    val district: String? = null,
    val state: String? = null,
    val country: String? = null,

    // Profile statistics
    val totalPosts: Int = 0,
    val acks: Int = 0,

    // Timestamps (in milliseconds since epoch)
    val updatedAt: Long = 0L,
    val lastSyncedAt: Long = 0L
)

fun ProfileEntity.toProfile(): Profile {
    return Profile(
        imageUrl = imageUrl ?: "",
        name = name,
        location = location ?: "",
        locality = locality ?: "",
        district = district ?: "",
        state = state ?: "",
        country = country ?: "",
        totalPosts = totalPosts,
        acks = acks,
        postByArea = listOf(0, 0, 0, 0), // Default or calculated
        myPosts = emptyList(),
        ackPosts = emptyList()
    )
}

fun Profile.toEntity(userId: String = "current_user"): ProfileEntity {
    return ProfileEntity(
        userId = userId,
        name = name,
        imageUrl = imageUrl,
        location = location,
        locality = locality,
        district = district,
        state = state,
        country = country,
        totalPosts = totalPosts,
        acks = acks,
        updatedAt = 0L,
        lastSyncedAt = 0L
    )
}
