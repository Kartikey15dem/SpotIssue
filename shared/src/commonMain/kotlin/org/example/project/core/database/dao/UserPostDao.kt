package org.example.project.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import org.example.project.core.database.entities.UserPostEntity

/**
 * DAO for user posts operations
 */
@Dao
interface UserPostDao {

    @Upsert
    suspend fun upsertPost(post: UserPostEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<UserPostEntity>)

    @Query("SELECT * FROM user_posts WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getUserPosts(userId: String = "current_user"): List<UserPostEntity>

    @Query("SELECT * FROM user_posts WHERE userId = :userId ORDER BY createdAt ASC")
    suspend fun getUserPostsOldest(userId: String = "current_user"): List<UserPostEntity>

    @Query("SELECT * FROM user_posts WHERE userId = :userId ORDER BY (likes + comments) DESC")
    suspend fun getUserPostsPopular(userId: String = "current_user"): List<UserPostEntity>

    @Query("SELECT * FROM user_posts WHERE id = :postId")
    suspend fun getPostById(postId: String): UserPostEntity?

    @Query("DELETE FROM user_posts WHERE id = :postId")
    suspend fun deletePost(postId: String)

    @Query("DELETE FROM user_posts WHERE userId = :userId")
    suspend fun deleteAllUserPosts(userId: String = "current_user")

    @Query("UPDATE user_posts SET likes = :likes, isLiked = :isLiked WHERE id = :postId")
    suspend fun updatePostLikeStatus(postId: String, likes: Int, isLiked: Boolean)

    @Query("SELECT COUNT(*) FROM user_posts WHERE userId = :userId")
    suspend fun getUserPostCount(userId: String = "current_user"): Int
}

