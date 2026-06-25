package org.example.project.core.data.local

import kotlinx.coroutines.flow.Flow
import org.example.project.core.database.dao.LikedPostDao
import org.example.project.core.database.dao.ProfileDao
import org.example.project.core.database.dao.UserPostDao
import org.example.project.core.database.entities.ProfileEntity
import org.example.project.core.data.mappers.Sort
import org.example.project.core.data.mappers.toLikedPostEntity
import org.example.project.core.data.mappers.toPost
import org.example.project.core.data.mappers.toUserPostEntity
import org.example.project.core.model.home.Post
import kotlin.time.Clock

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
    fun getProfileFlow(): Flow<ProfileEntity?> {
        return profileDao.getProfileFlow()
    }

    suspend fun clearProfile() {
        profileDao.deleteProfile()
    }


}