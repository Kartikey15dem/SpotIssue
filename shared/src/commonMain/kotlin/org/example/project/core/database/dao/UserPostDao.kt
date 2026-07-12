package org.example.project.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import org.example.project.core.database.entities.UserPostEntity

@Dao
interface UserPostDao {

    @Upsert
    suspend fun upsertPost(post: UserPostEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<UserPostEntity>)

    @Query("SELECT * FROM user_posts WHERE sort = :sort ORDER BY createdAt DESC LIMIT :limit")
    fun observeNewest(sort: String, limit: Int): Flow<List<UserPostEntity>>

    @Query("SELECT * FROM user_posts WHERE sort = :sort AND (createdAt < :anchorCreatedAt OR (createdAt = :anchorCreatedAt AND id < :anchorId)) ORDER BY createdAt DESC, id DESC LIMIT :limit")
    fun observeAfterAnchor(sort: String, anchorCreatedAt: Long, anchorId: String, limit: Int): Flow<List<UserPostEntity>>

    @Query("SELECT * FROM user_posts ORDER BY createdAt DESC")
    suspend fun getUserPosts(): List<UserPostEntity>

    @Query("SELECT * FROM user_posts WHERE id = :postId")
    suspend fun getPostById(postId: String): UserPostEntity?

    @Query("DELETE FROM user_posts WHERE id = :postId")
    suspend fun deletePost(postId: String)

    @Query("DELETE FROM user_posts WHERE sort = :sort")
    suspend fun deleteAllUserPosts(sort: String)

    @Query("DELETE FROM user_posts")
    suspend fun clearAll()

    @Query("DELETE FROM user_posts WHERE sort = :sort AND id NOT IN (SELECT id FROM user_posts WHERE sort = :sort ORDER BY createdAt DESC LIMIT :maxPosts)")
    suspend fun trimUserPosts(maxPosts: Int, sort: String)

    @Query("UPDATE user_posts SET likes = :likes, isLiked = :isLiked WHERE id = :postId")
    suspend fun updatePostLikeStatus(postId: String, likes: Int, isLiked: Boolean)

    @Query("UPDATE user_posts SET comments = :commentsCount WHERE id = :postId")
    suspend fun updateCommentsCount(postId: String, commentsCount: Int)

    @Query("UPDATE user_posts SET isReported = :isReported WHERE id = :postId")
    suspend fun updateReportStatus(postId: String, isReported: Boolean)

    @Query("UPDATE user_posts SET userName = :name, userAvatar = :avatar WHERE userId = :ownerId")
    suspend fun updateUserInfo(ownerId: String, name: String, avatar: String?)

    @Query("SELECT COUNT(*) FROM user_posts WHERE sort = :sort")
    suspend fun getUserPostCount(sort: String): Int
}
