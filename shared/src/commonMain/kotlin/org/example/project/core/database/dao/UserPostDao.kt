package org.example.project.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import androidx.paging.PagingSource
import kotlinx.coroutines.flow.Flow
import org.example.project.core.database.entities.UserPostEntity

@Dao
interface UserPostDao {

    @Upsert
    suspend fun upsertPost(post: UserPostEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<UserPostEntity>)

    @Query("SELECT * FROM user_posts ORDER BY createdAt DESC")
    fun pagingSource(): PagingSource<Int, UserPostEntity>

    @Query("SELECT * FROM user_posts ORDER BY createdAt ASC")
    fun pagingSourceOldest(): PagingSource<Int, UserPostEntity>

    @Query("SELECT * FROM user_posts ORDER BY (likes + comments) DESC")
    fun pagingSourcePopular(): PagingSource<Int, UserPostEntity>

    @Query("SELECT * FROM user_posts ORDER BY createdAt DESC")
    suspend fun getUserPosts(): List<UserPostEntity>

    @Query("SELECT * FROM user_posts ORDER BY createdAt DESC")
    fun observeUserPosts(): Flow<List<UserPostEntity>>

    @Query("SELECT * FROM user_posts ORDER BY createdAt ASC")
    fun observeUserPostsOldest(): Flow<List<UserPostEntity>>

    @Query("SELECT * FROM user_posts ORDER BY (likes + comments) DESC")
    fun observeUserPostsPopular(): Flow<List<UserPostEntity>>

    @Query("SELECT * FROM user_posts ORDER BY createdAt ASC")
    suspend fun getUserPostsOldest(): List<UserPostEntity>

    @Query("SELECT * FROM user_posts ORDER BY (likes + comments) DESC")
    suspend fun getUserPostsPopular(): List<UserPostEntity>

    @Query("SELECT * FROM user_posts WHERE id = :postId")
    suspend fun getPostById(postId: String): UserPostEntity?

    @Query("DELETE FROM user_posts WHERE id = :postId")
    suspend fun deletePost(postId: String)

    @Query("DELETE FROM user_posts")
    suspend fun deleteAllUserPosts()

    @Query("DELETE FROM user_posts WHERE id NOT IN (SELECT id FROM user_posts ORDER BY createdAt DESC LIMIT :maxPosts)")
    suspend fun trimUserPosts(maxPosts: Int)

    @Query("UPDATE user_posts SET likes = :likes, isLiked = :isLiked WHERE id = :postId")
    suspend fun updatePostLikeStatus(postId: String, likes: Int, isLiked: Boolean)

    @Query("UPDATE user_posts SET comments = :commentsCount WHERE id = :postId")
    suspend fun updateCommentsCount(postId: String, commentsCount: Int)

    @Query("UPDATE user_posts SET isReported = :isReported WHERE id = :postId")
    suspend fun updateReportStatus(postId: String, isReported: Boolean)

    @Query("UPDATE user_posts SET userName = :name, userAvatar = :avatar WHERE userId = :ownerId")
    suspend fun updateUserInfo(ownerId: String, name: String, avatar: String?)

    @Query("SELECT COUNT(*) FROM user_posts")
    suspend fun getUserPostCount(): Int
}
