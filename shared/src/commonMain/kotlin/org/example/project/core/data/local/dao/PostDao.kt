package org.example.project.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.example.project.core.data.local.entities.PostEntity

@Dao
interface PostDao {
    /**
     * Get all posts for a specific post level
     */
    @Query("SELECT * FROM posts WHERE postLevel = :postLevel ORDER BY cachedAt DESC")
    suspend fun getPostsByLevel(postLevel: String): List<PostEntity>

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

