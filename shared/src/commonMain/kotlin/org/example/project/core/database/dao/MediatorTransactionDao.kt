package org.example.project.core.database.dao

import androidx.room.Dao
import androidx.room.Transaction
import org.example.project.core.database.entities.PostEntity
import org.example.project.core.database.entities.LikedPostEntity
import org.example.project.core.database.entities.UserPostEntity
import org.example.project.core.database.entities.RemoteKeysEntity

@Dao
interface MediatorTransactionDao {
    
    @Transaction
    suspend fun refreshFeed(
        postDao: PostDao,
        remoteKeysDao: RemoteKeysDao,
        level: String,
        keyType: String,
        posts: List<PostEntity>,
        remoteKeys: List<RemoteKeysEntity>
    ) {
        remoteKeysDao.clearRemoteKeys(keyType)
        postDao.deletePostsByLevel(level)
        
        remoteKeysDao.insertAll(remoteKeys)
        postDao.insertPosts(posts)
    }

    @Transaction
    suspend fun appendPage(
        postDao: PostDao,
        remoteKeysDao: RemoteKeysDao,
        posts: List<PostEntity>,
        remoteKeys: List<RemoteKeysEntity>,
        level: String,
        maxCachedPosts: Int
    ) {
        remoteKeysDao.insertAll(remoteKeys)
        postDao.insertPosts(posts)
        postDao.trimPostsByLevel(level, maxCachedPosts)
    }

    @Transaction
    suspend fun refreshUserPosts(
        userPostDao: UserPostDao,
        remoteKeysDao: RemoteKeysDao,
        keyType: String,
        posts: List<UserPostEntity>,
        remoteKeys: List<RemoteKeysEntity>
    ) {
        remoteKeysDao.clearRemoteKeys(keyType)
        userPostDao.deleteAllUserPosts()
        
        remoteKeysDao.insertAll(remoteKeys)
        userPostDao.insertPosts(posts)
    }

    @Transaction
    suspend fun appendUserPage(
        userPostDao: UserPostDao,
        remoteKeysDao: RemoteKeysDao,
        posts: List<UserPostEntity>,
        remoteKeys: List<RemoteKeysEntity>,
        maxCachedPosts: Int
    ) {
        remoteKeysDao.insertAll(remoteKeys)
        userPostDao.insertPosts(posts)
        userPostDao.trimUserPosts(maxPosts = maxCachedPosts)
    }

    @Transaction
    suspend fun refreshLikedPosts(
        likedPostDao: LikedPostDao,
        remoteKeysDao: RemoteKeysDao,
        keyType: String,
        posts: List<LikedPostEntity>,
        remoteKeys: List<RemoteKeysEntity>
    ) {
        remoteKeysDao.clearRemoteKeys(keyType)
        likedPostDao.deleteAllLikedPosts()
        
        remoteKeysDao.insertAll(remoteKeys)
        likedPostDao.insertPosts(posts)
    }

    @Transaction
    suspend fun appendLikedPage(
        likedPostDao: LikedPostDao,
        remoteKeysDao: RemoteKeysDao,
        posts: List<LikedPostEntity>,
        remoteKeys: List<RemoteKeysEntity>,
        maxCachedPosts: Int
    ) {
        remoteKeysDao.insertAll(remoteKeys)
        likedPostDao.insertPosts(posts)
        likedPostDao.trimLikedPosts(maxPosts = maxCachedPosts)
    }
}
