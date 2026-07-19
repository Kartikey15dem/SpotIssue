package org.example.project.core.data.local

import kotlinx.coroutines.flow.Flow
import org.example.project.core.database.dao.ProfileDao
import org.example.project.core.database.entities.ProfileEntity

/**
 * Local data source for profile operations
 */
class ProfileLocalDataSource(
    private val profileDao: ProfileDao,
) {
    /**
     * Save or update profile
     */
    suspend fun saveProfile(profile: ProfileEntity) {
        profileDao.upsertProfile(profile)
    }

    /**
     * Get profile as Flow
     */
    fun getProfileFlow(): Flow<ProfileEntity?> = profileDao.getProfileFlow()

    suspend fun clearProfile() {
        profileDao.deleteProfile()
    }
}
