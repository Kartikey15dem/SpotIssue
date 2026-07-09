package org.example.project.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.paging.PagingSource
import kotlinx.coroutines.flow.Flow
import org.example.project.core.database.entities.PostEntity

@Dao
interface PostDao {
    /**
     * Get all posts for a specific post level
     */
    @Query("SELECT * FROM posts WHERE postLevel = :postLevel ORDER BY cachedAt DESC, id DESC")
    suspend fun getPostsByLevel(postLevel: String): List<PostEntity>

    /**
     * Paging Source for posts
     */
    @Query("SELECT * FROM posts WHERE postLevel = :postLevel ORDER BY cachedAt DESC")
    fun pagingSourceByLevel(postLevel: String): PagingSource<Int, PostEntity>

    /**
     * Observe posts for a specific post level
     */
    @Query("SELECT * FROM posts WHERE postLevel = :postLevel ORDER BY cachedAt DESC")
    fun observePostsByLevel(postLevel: String): Flow<List<PostEntity>>

    /**
     * Get post by ID
     */
    @Query("SELECT * FROM posts WHERE id = :postId")
    suspend fun getPostById(postId: String): PostEntity?

    /**
     * Observe post by ID
     */
    @Query("SELECT * FROM posts WHERE id = :postId")
    fun observePost(postId: String): Flow<PostEntity?>

    /**
     * Insert posts (replace if conflict)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<PostEntity>)

    /**
     * Delete all posts for a specific level
     */
    @Query("DELETE FROM posts WHERE postLevel = :postLevel")
    suspend fun deletePostsByLevel(postLevel: String)

    /**
     * Keep only the newest [maxPosts] posts for a given level, deleting the rest.
     */
    @Query(
        """
        DELETE FROM posts
        WHERE postLevel = :postLevel
          AND id NOT IN (
            SELECT id FROM posts
            WHERE postLevel = :postLevel
            ORDER BY cachedAt DESC
            LIMIT :maxPosts
          )
        """,
    )
    suspend fun trimPostsByLevel(postLevel: String, maxPosts: Int)

    /**
     * Update like status and count for a post
     */
    @Query("UPDATE posts SET likes = :likesCount, isLiked = :isLiked WHERE id = :postId")
    suspend fun updateLikeStatus(postId: String, likesCount: Int, isLiked: Boolean)

    /**
     * Update report status for a post
     */
    @Query("UPDATE posts SET isReported = :isReported WHERE id = :postId")
    suspend fun updateReportStatus(postId: String, isReported: Boolean)

    /**
     * Delete a post by ID
     */
    @Query("DELETE FROM posts WHERE id = :postId")
    suspend fun deletePostById(postId: String)

    /**
     * Update comments count for a post
     */
    @Query("UPDATE posts SET comments = :commentsCount WHERE id = :postId")
    suspend fun updateCommentsCount(postId: String, commentsCount: Int)

    /**
     * Clear all posts
     */
    @Query("DELETE FROM posts")
    suspend fun clearAll()

    /**
     * Get count of cached posts for a level
     */
    @Query("SELECT COUNT(*) FROM posts WHERE postLevel = :postLevel")
    suspend fun getPostCountByLevel(postLevel: String): Int
}
