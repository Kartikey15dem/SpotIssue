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
    @Query("SELECT * FROM posts WHERE postLevel = :postLevel ORDER BY cachedAt DESC")
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

