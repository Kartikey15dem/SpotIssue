package org.example.project.core.database.dao

import androidx.room.Dao
import androidx.room.Transaction
import org.example.project.core.database.entities.PostEntity
import org.example.project.core.database.entities.LikedPostEntity
import org.example.project.core.database.entities.UserPostEntity
import org.example.project.core.database.entities.RemoteKeysEntity

private const val DB_TRACE = "[DB_TRACE]"

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
        println("""
$DB_TRACE refreshFeed()
$DB_TRACE posts=${posts.size}
$DB_TRACE time=${kotlin.time.Clock.System.now().toEpochMilliseconds()}
$DB_TRACE =========================
""")
        println("$DB_TRACE clearRemoteKeys")
        remoteKeysDao.clearRemoteKeys(keyType)
        
        println("$DB_TRACE deletePostsByLevel")
        postDao.deletePostsByLevel(level)
        
        println("$DB_TRACE insertRemoteKeys")
        remoteKeysDao.insertAll(remoteKeys)
        
        println("$DB_TRACE insertPosts")
        println("""
$DB_TRACE INSERTING POSTS
$DB_TRACE count=${posts.size}
$DB_TRACE ids=
${posts.joinToString("\n") { "$DB_TRACE " + it.id }}
$DB_TRACE time=${kotlin.time.Clock.System.now().toEpochMilliseconds()}
$DB_TRACE =========================
""")
        postDao.insertPosts(posts)
        
        println("""
$DB_TRACE refreshFeed FINISHED
$DB_TRACE time=${kotlin.time.Clock.System.now().toEpochMilliseconds()}
$DB_TRACE =========================
""")
    }

    @Transaction
    suspend fun appendPage(
        postDao: PostDao,
        remoteKeysDao: RemoteKeysDao,
        posts: List<PostEntity>,
        remoteKeys: List<RemoteKeysEntity>,
        level: String,
        keyType: String,
        maxCachedPosts: Int
    ) {
        println("""
$DB_TRACE appendPage()
$DB_TRACE posts=${posts.size}
$DB_TRACE first=${posts.firstOrNull()?.id}
$DB_TRACE last=${posts.lastOrNull()?.id}
$DB_TRACE time=${kotlin.time.Clock.System.now().toEpochMilliseconds()}
$DB_TRACE =========================
""")
        println("$DB_TRACE insertRemoteKeys")
        remoteKeysDao.insertAll(remoteKeys)
        
        println("$DB_TRACE insertPosts")
        println("""
$DB_TRACE INSERTING POSTS
$DB_TRACE count=${posts.size}
$DB_TRACE ids=
${posts.joinToString("\n") { "$DB_TRACE " + it.id }}
$DB_TRACE time=${kotlin.time.Clock.System.now().toEpochMilliseconds()}
$DB_TRACE =========================
""")
        postDao.insertPosts(posts)
        
        println("""
$DB_TRACE appendPage FINISHED
$DB_TRACE time=${kotlin.time.Clock.System.now().toEpochMilliseconds()}
$DB_TRACE =========================
""")
    }

    @Transaction
    suspend fun refreshUserPosts(
        userPostDao: UserPostDao,
        remoteKeysDao: RemoteKeysDao,
        keyType: String,
        sort: String,
        posts: List<UserPostEntity>,
        remoteKeys: List<RemoteKeysEntity>
    ) {
        remoteKeysDao.clearRemoteKeys(keyType)
        userPostDao.deleteAllUserPosts(sort)
        
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
        sort: String,
        posts: List<LikedPostEntity>,
        remoteKeys: List<RemoteKeysEntity>
    ) {
        remoteKeysDao.clearRemoteKeys(keyType)
        likedPostDao.deleteAllLikedPosts(sort)
        
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
