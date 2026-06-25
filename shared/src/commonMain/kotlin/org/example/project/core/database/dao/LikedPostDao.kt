package org.example.project.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import androidx.paging.PagingSource
import kotlinx.coroutines.flow.Flow
import org.example.project.core.database.entities.LikedPostEntity

@Dao
interface LikedPostDao {

    @Upsert
    suspend fun upsertPost(post: LikedPostEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<LikedPostEntity>)

    @Query("SELECT * FROM liked_posts ORDER BY createdAt DESC")
    fun pagingSource(): PagingSource<Int, LikedPostEntity>

    @Query("SELECT * FROM liked_posts ORDER BY createdAt ASC")
    fun pagingSourceOldest(): PagingSource<Int, LikedPostEntity>

    @Query("SELECT * FROM liked_posts ORDER BY (likes + comments) DESC")
    fun pagingSourcePopular(): PagingSource<Int, LikedPostEntity>

    @Query("SELECT * FROM liked_posts ORDER BY createdAt DESC")
    suspend fun getLikedPosts(): List<LikedPostEntity>

    @Query("SELECT * FROM liked_posts ORDER BY createdAt DESC")
    fun observeLikedPosts(): Flow<List<LikedPostEntity>>

    @Query("SELECT * FROM liked_posts ORDER BY likedAt ASC")
    fun observeLikedPostsOldest(): Flow<List<LikedPostEntity>>

    @Query("SELECT * FROM liked_posts ORDER BY (likes + comments) DESC")
    fun observeLikedPostsPopular(): Flow<List<LikedPostEntity>>

    @Query("SELECT * FROM liked_posts ORDER BY likedAt ASC")
    suspend fun getLikedPostsOldest(): List<LikedPostEntity>

    @Query("SELECT * FROM liked_posts ORDER BY (likes + comments) DESC")
    suspend fun getLikedPostsPopular(): List<LikedPostEntity>

    @Query("SELECT * FROM liked_posts WHERE id = :postId")
    suspend fun getLikedPostById(postId: String): LikedPostEntity?

    @Query("DELETE FROM liked_posts WHERE id = :postId")
    suspend fun deleteLikedPost(postId: String)

    @Query("DELETE FROM liked_posts")
    suspend fun deleteAllLikedPosts()

    @Query("DELETE FROM liked_posts WHERE id NOT IN (SELECT id FROM liked_posts ORDER BY createdAt DESC LIMIT :maxPosts)")
    suspend fun trimLikedPosts(maxPosts: Int)

    @Query("UPDATE liked_posts SET likes = :likes, isLiked = :isLiked WHERE id = :postId")
    suspend fun updatePostLikeStatus(postId: String, likes: Int, isLiked: Boolean)

    @Query("UPDATE liked_posts SET comments = :commentsCount WHERE id = :postId")
    suspend fun updateCommentsCount(postId: String, commentsCount: Int)

    @Query("UPDATE liked_posts SET isReported = :isReported WHERE id = :postId")
    suspend fun updateReportStatus(postId: String, isReported: Boolean)

    @Query("UPDATE liked_posts SET userName = :name, userAvatar = :avatar WHERE userId = :ownerId")
    suspend fun updateUserInfo(ownerId: String, name: String, avatar: String?)

    @Query("SELECT COUNT(*) FROM liked_posts")
    suspend fun getLikedPostCount(): Int
}
