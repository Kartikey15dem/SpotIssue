package org.example.project.core.database


import androidx.room.Database
import androidx.room.RoomDatabase

import org.example.project.core.database.dao.ProfileDao
import org.example.project.core.database.dao.PostDao
import org.example.project.core.database.dao.CacheMetadataDao
import org.example.project.core.database.dao.ActiveIssuesDao
import org.example.project.core.database.dao.UserPostDao
import org.example.project.core.database.dao.LikedPostDao
import org.example.project.core.database.entities.ProfileEntity
import org.example.project.core.database.entities.PostEntity
import org.example.project.core.database.entities.CacheMetadataEntity
import org.example.project.core.database.entities.ActiveIssuesEntity
import org.example.project.core.database.entities.UserPostEntity
import org.example.project.core.database.entities.LikedPostEntity

@Database(
    entities = [
        ProfileEntity::class,
        PostEntity::class,
        CacheMetadataEntity::class,
        ActiveIssuesEntity::class,
        UserPostEntity::class,
        LikedPostEntity::class
    ],
    version = 3,
    exportSchema = true
)
actual abstract class IssueSpotDatabase : RoomDatabase() {
    actual abstract fun profileDao(): ProfileDao
    actual abstract fun postDao(): PostDao
    actual abstract fun cacheMetadataDao(): CacheMetadataDao
    actual abstract fun activeIssuesDao(): ActiveIssuesDao
    actual abstract fun userPostDao(): UserPostDao
    actual abstract fun likedPostDao(): LikedPostDao

}



