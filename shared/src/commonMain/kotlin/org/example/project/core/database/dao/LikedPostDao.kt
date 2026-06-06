package org.example.project.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import androidx.paging.PagingSource
import org.example.project.core.database.entities.LikedPostEntity

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
    fun pagingSource(userId: String = "current_user"): PagingSource<Int, LikedPostEntity>

    @Query("SELECT * FROM liked_posts WHERE userId = :userId ORDER BY likedAt ASC")
    fun pagingSourceOldest(userId: String = "current_user"): PagingSource<Int, LikedPostEntity>

    @Query("SELECT * FROM liked_posts WHERE userId = :userId ORDER BY (likes + comments) DESC")
    fun pagingSourcePopular(userId: String = "current_user"): PagingSource<Int, LikedPostEntity>

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

    /**
     * Keep only the newest [maxPosts] rows for this user, deleting the rest.
     */
    @Query(
        """
        DELETE FROM liked_posts
        WHERE userId = :userId
          AND id NOT IN (
            SELECT id FROM liked_posts
            WHERE userId = :userId
            ORDER BY likedAt DESC
            LIMIT :maxPosts
          )
        """,
    )
    suspend fun trimLikedPosts(userId: String = "current_user", maxPosts: Int)

    @Query("UPDATE liked_posts SET likes = :likes, isLiked = :isLiked WHERE id = :postId")
    suspend fun updatePostLikeStatus(postId: String, likes: Int, isLiked: Boolean)

    @Query("UPDATE liked_posts SET comments = :commentsCount WHERE id = :postId")
    suspend fun updateCommentsCount(postId: String, commentsCount: Int)

    @Query("UPDATE liked_posts SET isReported = :isReported WHERE id = :postId")
    suspend fun updateReportStatus(postId: String, isReported: Boolean)

    @Query("SELECT COUNT(*) FROM liked_posts WHERE userId = :userId")
    suspend fun getLikedPostCount(userId: String = "current_user"): Int
}
