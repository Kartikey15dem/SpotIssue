package org.example.project.core.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

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

