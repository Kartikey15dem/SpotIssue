package org.example.project.core.database.dao

import androidx.room.Dao
import androidx.room.Transaction
import org.example.project.core.database.entities.PostEntity
import org.example.project.core.database.entities.LikedPostEntity
import org.example.project.core.database.entities.UserPostEntity
import org.example.project.core.database.entities.RemoteKeysEntity

@Dao
/**
 * ===================================================================================
 * SECTION: KMP COMPATIBLE ATOMIC TRANSACTIONS
 * ===================================================================================
 * This DAO orchestrates cross-table database operations securely within a single transaction.
 * 
 * Why this exists:
 * The standard Android `RoomDatabase.withTransaction {}` block is not fully supported
 * by the `BundledSQLiteDriver` used in Kotlin Multiplatform (KMP). It causes crashes.
 * By using the `@Transaction` annotation on DAO methods, we ensure that clearing old 
 * Paging data and inserting new network data happens atomically across all platforms
 * (Android and iOS). This fundamentally prevents "flickering" or blank screens when 
 * the feed refreshes.
 */
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
    }
}
