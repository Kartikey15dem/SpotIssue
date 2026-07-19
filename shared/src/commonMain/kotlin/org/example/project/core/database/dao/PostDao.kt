package org.example.project.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.example.project.core.database.entities.PostEntity

@Dao
interface PostDao {
    /**
     * Observe newest posts for a specific post level
     */
    @Query("SELECT * FROM posts WHERE postLevel = :postLevel ORDER BY cachedAt DESC, id DESC LIMIT :limit")
    fun observeNewestByLevel(
        postLevel: String,
        limit: Int,
    ): Flow<List<PostEntity>>

    /**
     * Observe posts starting from an anchor (for sliding window)
     */
    @Query(
        "SELECT * FROM posts WHERE postLevel = :postLevel AND (cachedAt < :anchorCachedAt OR (cachedAt = :anchorCachedAt AND id < :anchorId)) ORDER BY cachedAt DESC, id DESC LIMIT :limit",
    )
    fun observeAfterAnchorByLevel(
        postLevel: String,
        anchorCachedAt: Long,
        anchorId: String,
        limit: Int,
    ): Flow<List<PostEntity>>

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
            ORDER BY cachedAt DESC, id DESC
            LIMIT :maxPosts
          )
        """,
    )
    suspend fun trimPostsByLevel(
        postLevel: String,
        maxPosts: Int,
    )

    /**
     * Update like status and count for a post
     */
    @Query("UPDATE posts SET likes = :likesCount, isLiked = :isLiked WHERE id = :postId")
    suspend fun updateLikeStatus(
        postId: String,
        likesCount: Int,
        isLiked: Boolean,
    )

    /**
     * Update report status for a post
     */
    @Query("UPDATE posts SET isReported = :isReported WHERE id = :postId")
    suspend fun updateReportStatus(
        postId: String,
        isReported: Boolean,
    )

    /**
     * Delete a post by ID
     */
    @Query("DELETE FROM posts WHERE id = :postId")
    suspend fun deletePostById(postId: String)

    /**
     * Update comments count for a post
     */
    @Query("UPDATE posts SET comments = :commentsCount WHERE id = :postId")
    suspend fun updateCommentsCount(
        postId: String,
        commentsCount: Int,
    )

    /**
     * Get count of cached posts for a level
     */
    @Query("SELECT COUNT(*) FROM posts WHERE postLevel = :postLevel")
    suspend fun getPostCountByLevel(postLevel: String): Int
}
