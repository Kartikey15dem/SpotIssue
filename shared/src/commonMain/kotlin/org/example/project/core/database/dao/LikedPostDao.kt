package org.example.project.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import org.example.project.core.database.entities.LikedPostEntity

@Dao
interface LikedPostDao {
    @Upsert
    suspend fun upsertPost(post: LikedPostEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<LikedPostEntity>)

    @Query(
        """SELECT * FROM liked_posts ORDER BY 
        CASE WHEN :sort = 'latest' THEN createdAt END DESC,
        CASE WHEN :sort = 'oldest' THEN createdAt END ASC,
        CASE WHEN :sort = 'popular' THEN likes END DESC,
        cachedAt DESC LIMIT :limit""",
    )
    fun observeNewest(
        sort: String,
        limit: Int,
    ): Flow<List<LikedPostEntity>>

    @Query(
        """SELECT * FROM liked_posts 
        WHERE (cachedAt < :anchorCachedAt OR (cachedAt = :anchorCachedAt AND id < :anchorId)) 
        ORDER BY 
        CASE WHEN :sort = 'latest' THEN createdAt END DESC,
        CASE WHEN :sort = 'oldest' THEN createdAt END ASC,
        CASE WHEN :sort = 'popular' THEN likes END DESC,
        cachedAt DESC LIMIT :limit""",
    )
    fun observeAfterAnchor(
        sort: String,
        anchorCachedAt: Long,
        anchorId: String,
        limit: Int,
    ): Flow<List<LikedPostEntity>>

    @Query("SELECT MIN(cachedAt) FROM liked_posts")
    suspend fun getMinCachedAt(): Long?

    @Query("SELECT * FROM liked_posts ORDER BY cachedAt DESC")
    suspend fun getLikedPosts(): List<LikedPostEntity>

    @Query("SELECT * FROM liked_posts WHERE id = :postId")
    suspend fun getLikedPostById(postId: String): LikedPostEntity?

    @Query("DELETE FROM liked_posts WHERE id = :postId")
    suspend fun deleteLikedPost(postId: String)

    @Query("DELETE FROM liked_posts")
    suspend fun deleteAllLikedPosts()

    @Query("DELETE FROM liked_posts")
    suspend fun clearAll()

    @Query("DELETE FROM liked_posts WHERE id NOT IN (SELECT id FROM liked_posts ORDER BY cachedAt DESC LIMIT :maxPosts)")
    suspend fun trimLikedPosts(maxPosts: Int)

    @Query("UPDATE liked_posts SET likes = :likes, isLiked = :isLiked WHERE id = :postId")
    suspend fun updatePostLikeStatus(
        postId: String,
        likes: Int,
        isLiked: Boolean,
    )

    @Query("UPDATE liked_posts SET comments = :commentsCount WHERE id = :postId")
    suspend fun updateCommentsCount(
        postId: String,
        commentsCount: Int,
    )

    @Query("UPDATE liked_posts SET isReported = :isReported WHERE id = :postId")
    suspend fun updateReportStatus(
        postId: String,
        isReported: Boolean,
    )

    @Query("UPDATE liked_posts SET userName = :name, userAvatar = :avatar WHERE userId = :ownerId")
    suspend fun updateUserInfo(
        ownerId: String,
        name: String,
        avatar: String?,
    )

    @Query("SELECT COUNT(*) FROM liked_posts")
    suspend fun getLikedPostCount(): Int
}
