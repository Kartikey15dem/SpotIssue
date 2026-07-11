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

    @Query("SELECT * FROM liked_posts WHERE sort = :sort ORDER BY createdAt DESC")
    fun pagingSource(sort: String): PagingSource<Int, LikedPostEntity>

    @Query("SELECT * FROM liked_posts WHERE sort = :sort ORDER BY createdAt ASC")
    fun pagingSourceOldest(sort: String): PagingSource<Int, LikedPostEntity>

    @Query("SELECT * FROM liked_posts WHERE sort = :sort ORDER BY likes DESC, createdAt DESC")
    fun pagingSourcePopular(sort: String): PagingSource<Int, LikedPostEntity>

    @Query("SELECT * FROM liked_posts ORDER BY createdAt DESC")
    suspend fun getLikedPosts(): List<LikedPostEntity>

    @Query("SELECT * FROM liked_posts WHERE sort = :sort ORDER BY createdAt DESC")
    fun observeLikedPosts(sort: String): Flow<List<LikedPostEntity>>

    @Query("SELECT * FROM liked_posts WHERE sort = :sort ORDER BY likedAt ASC")
    fun observeLikedPostsOldest(sort: String): Flow<List<LikedPostEntity>>

    @Query("SELECT * FROM liked_posts WHERE sort = :sort ORDER BY likes DESC, createdAt DESC")
    fun observeLikedPostsPopular(sort: String): Flow<List<LikedPostEntity>>

    @Query("SELECT * FROM liked_posts ORDER BY likedAt ASC")
    suspend fun getLikedPostsOldest(): List<LikedPostEntity>

    @Query("SELECT * FROM liked_posts WHERE sort = :sort ORDER BY likes DESC, createdAt DESC")
    suspend fun getLikedPostsPopular(sort: String): List<LikedPostEntity>

    @Query("SELECT * FROM liked_posts WHERE id = :postId")
    suspend fun getLikedPostById(postId: String): LikedPostEntity?

    @Query("DELETE FROM liked_posts WHERE id = :postId")
    suspend fun deleteLikedPost(postId: String)

    @Query("DELETE FROM liked_posts WHERE sort = :sort")
    suspend fun deleteAllLikedPosts(sort: String)

    @Query("DELETE FROM liked_posts")
    suspend fun clearAll()

    @Query("DELETE FROM liked_posts WHERE sort = :sort AND id NOT IN (SELECT id FROM liked_posts WHERE sort = :sort ORDER BY createdAt DESC LIMIT :maxPosts)")
    suspend fun trimLikedPosts(maxPosts: Int, sort: String)

    @Query("UPDATE liked_posts SET likes = :likes, isLiked = :isLiked WHERE id = :postId")
    suspend fun updatePostLikeStatus(postId: String, likes: Int, isLiked: Boolean)

    @Query("UPDATE liked_posts SET comments = :commentsCount WHERE id = :postId")
    suspend fun updateCommentsCount(postId: String, commentsCount: Int)

    @Query("UPDATE liked_posts SET isReported = :isReported WHERE id = :postId")
    suspend fun updateReportStatus(postId: String, isReported: Boolean)

    @Query("UPDATE liked_posts SET userName = :name, userAvatar = :avatar WHERE userId = :ownerId")
    suspend fun updateUserInfo(ownerId: String, name: String, avatar: String?)

    @Query("SELECT COUNT(*) FROM liked_posts WHERE sort = :sort")
    suspend fun getLikedPostCount(sort: String): Int
}
