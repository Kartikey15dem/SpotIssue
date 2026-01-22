package org.example.project.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import org.example.project.core.data.local.entities.LikedPostEntity

/**
 * DAO for liked posts operations
 */
@Dao
interface LikedPostDao {

    @Upsert
    suspend fun upsertPost(post: LikedPostEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<LikedPostEntity>)

    @Query("SELECT * FROM liked_posts WHERE userId = :userId ORDER BY likedAt DESC")
    suspend fun getLikedPosts(userId: String = "current_user"): List<LikedPostEntity>

    @Query("SELECT * FROM liked_posts WHERE userId = :userId ORDER BY likedAt ASC")
    suspend fun getLikedPostsOldest(userId: String = "current_user"): List<LikedPostEntity>

    @Query("SELECT * FROM liked_posts WHERE userId = :userId ORDER BY (likes + comments) DESC")
    suspend fun getLikedPostsPopular(userId: String = "current_user"): List<LikedPostEntity>

    @Query("SELECT * FROM liked_posts WHERE id = :postId")
    suspend fun getLikedPostById(postId: String): LikedPostEntity?

    @Query("DELETE FROM liked_posts WHERE id = :postId")
    suspend fun deleteLikedPost(postId: String)

    @Query("DELETE FROM liked_posts WHERE userId = :userId")
    suspend fun deleteAllLikedPosts(userId: String = "current_user")

    @Query("UPDATE liked_posts SET likes = :likes WHERE id = :postId")
    suspend fun updatePostLikes(postId: String, likes: Int)

    @Query("SELECT COUNT(*) FROM liked_posts WHERE userId = :userId")
    suspend fun getLikedPostCount(userId: String = "current_user"): Int
}

