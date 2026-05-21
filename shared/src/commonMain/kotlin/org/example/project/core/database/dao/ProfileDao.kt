package org.example.project.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import org.example.project.core.database.entities.ProfileEntity

/**
 * DAO for profile data operations
 */
@Dao
interface ProfileDao {

    /**
     * Insert or update profile (upsert)
     */
    @Upsert
    suspend fun upsertProfile(profile: ProfileEntity)

    /**
     * Get current user profile
     */
    @Query("SELECT * FROM profile WHERE userId = 'current_user' LIMIT 1")
    suspend fun getProfile(): ProfileEntity?

    /**
     * Get current user profile as Flow
     */
    @Query("SELECT * FROM profile WHERE userId = 'current_user' LIMIT 1")
    fun getProfileFlow(): Flow<ProfileEntity?>

    /**
     * Delete profile
     */
    @Query("DELETE FROM profile WHERE userId = 'current_user'")
    suspend fun deleteProfile()

    /**
     * Update profile image
     */
    @Query("UPDATE profile SET imageUrl = :imageUrl, updatedAt = :timestamp WHERE userId = 'current_user'")
    suspend fun updateProfileImage(imageUrl: String?, timestamp: Long)

    /**
     * Update profile name
     */
    @Query("UPDATE profile SET name = :name, updatedAt = :timestamp WHERE userId = 'current_user'")
    suspend fun updateProfileName(name: String, timestamp: Long)
}

