package org.example.project.core.database

import org.example.project.core.database.dao.ProfileDao
import org.example.project.core.database.dao.PostDao
import org.example.project.core.database.dao.CacheMetadataDao
import org.example.project.core.database.dao.ActiveIssuesDao
import org.example.project.core.database.dao.UserPostDao
import org.example.project.core.database.dao.LikedPostDao
import org.example.project.core.database.dao.RemoteKeysDao



expect abstract class IssueSpotDatabase {
    abstract fun profileDao(): ProfileDao
    abstract fun postDao(): PostDao
    abstract fun cacheMetadataDao(): CacheMetadataDao
    abstract fun activeIssuesDao(): ActiveIssuesDao
    abstract fun userPostDao(): UserPostDao
    abstract fun likedPostDao(): LikedPostDao
    abstract fun remoteKeysDao(): RemoteKeysDao

}

