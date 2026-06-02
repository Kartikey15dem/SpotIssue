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
    val id: Int = 1,
    val userId : String = "current_user",
    val email: String? = null,
    val name: String,
    val imageUrl: String? = null,
    val totalPosts: Int = 0,
    val acks: Int = 0,
    val updatedAt: Long = 0L,
    val postByAreaStr: String? = null,
    val lastSyncedAt: Long = 0L
)

fun ProfileEntity.toProfile(): Profile {
    val postByArea = postByAreaStr?.split(",")?.mapNotNull { it.toIntOrNull() } ?: listOf(0, 0, 0, 0)
    return Profile(
        imageUrl = imageUrl ?: "",
        name = name,
        email = email ?: "",
        totalPosts = totalPosts,
        acks = acks,
        postByArea = if (postByArea.size == 4) postByArea else listOf(0, 0, 0, 0)
    )
}
