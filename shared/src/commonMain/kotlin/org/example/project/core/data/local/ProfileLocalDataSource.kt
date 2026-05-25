package org.example.project.core.data.local

import kotlinx.coroutines.flow.Flow
import org.example.project.core.database.dao.LikedPostDao
import org.example.project.core.database.dao.ProfileDao
import org.example.project.core.database.dao.UserPostDao
import org.example.project.core.database.entities.ProfileEntity
import org.example.project.core.data.mappers.Sort
import org.example.project.core.data.mappers.toLikedPostEntity
import org.example.project.core.data.mappers.toPost
import org.example.project.core.data.mappers.toUserPostEntity
import org.example.project.core.model.home.Post
import kotlin.time.Clock

/**
 * Local data source for profile operations
 */
class ProfileLocalDataSource(
    private val profileDao: ProfileDao,
    private val userPostDao: UserPostDao,
    private val likedPostDao: LikedPostDao
) {

    // ============ Profile Operations ============

    /**
     * Save or update profile
     */
    suspend fun saveProfile(profile: ProfileEntity) {
        profileDao.upsertProfile(profile)
    }

    /**
     * Get current profile
     */
    suspend fun getProfile(): ProfileEntity? {
        return profileDao.getProfile()
    }

    /**
     * Get profile as Flow
     */
    fun getProfileFlow(): Flow<ProfileEntity?> {
        return profileDao.getProfileFlow()
    }

    /**
     * Delete profile
     */
    suspend fun deleteProfile() {
        profileDao.deleteProfile()
    }

    /**
     * Update profile image
     */
    suspend fun updateProfileImage(imageUrl: String?) {
        val currentTime = getCurrentTimeMillis()
        profileDao.updateProfileImage(imageUrl, currentTime)
    }

    /**
     * Update profile name
     */
    suspend fun updateProfileName(name: String) {
        val currentTime = getCurrentTimeMillis()
        profileDao.updateProfileName(name, currentTime)
    }

    // ============ User Posts Operations ============

    /**
     * Cache user posts
     */
    suspend fun cacheUserPosts(posts: List<Post>) {
        val entities = posts.map { it.toUserPostEntity() }
        userPostDao.deleteAllUserPosts() // Clear old cache
        userPostDao.insertPosts(entities)
    }

    /**
     * Append (upsert) user posts without clearing the existing cache.
     */
    suspend fun appendUserPosts(posts: List<Post>) {
        val entities = posts.map { it.toUserPostEntity() }
        userPostDao.insertPosts(entities)
    }

    /**
     * Get cached user posts sorted
     */
    suspend fun getCachedUserPosts(sort: Sort): List<Post> {
        val entities = when (sort) {
            Sort.LATEST -> userPostDao.getUserPosts()
            Sort.OLDEST -> userPostDao.getUserPostsOldest()
            Sort.POPULAR -> userPostDao.getUserPostsPopular()
        }
        return entities.map { it.toPost() }
    }

    /**
     * Add a new user post to cache
     */
    suspend fun addUserPost(post: Post) {
        userPostDao.upsertPost(post.toUserPostEntity())
    }

    /**
     * Delete a user post from cache
     */
    suspend fun deleteUserPost(postId: String) {
        userPostDao.deletePost(postId)
    }

    /**
     * Update post like status in user posts
     */
    suspend fun updateUserPostLikeStatus(postId: String, likes: Int, isLiked: Boolean) {
        userPostDao.updatePostLikeStatus(postId, likes, isLiked)
    }

    /**
     * Get user post count
     */
    suspend fun getUserPostCount(): Int {
        return userPostDao.getUserPostCount()
    }

    // ============ Liked Posts Operations ============

    /**
     * Cache liked posts
     */
    suspend fun cacheLikedPosts(posts: List<Post>) {
        val entities = posts.map { it.toLikedPostEntity() }
        likedPostDao.deleteAllLikedPosts() // Clear old cache
        likedPostDao.insertPosts(entities)
    }

    /**
     * Append (upsert) liked posts without clearing the existing cache.
     */
    suspend fun appendLikedPosts(posts: List<Post>) {
        val entities = posts.map { it.toLikedPostEntity() }
        likedPostDao.insertPosts(entities)
    }

    /**
     * Get cached liked posts sorted
     */
    suspend fun getCachedLikedPosts(sort: Sort): List<Post> {
        val entities = when (sort) {
            Sort.LATEST -> likedPostDao.getLikedPosts()
            Sort.OLDEST -> likedPostDao.getLikedPostsOldest()
            Sort.POPULAR -> likedPostDao.getLikedPostsPopular()
        }
        return entities.map { it.toPost() }
    }

    /**
     * Add a liked post to cache
     */
    suspend fun addLikedPost(post: Post) {
        likedPostDao.upsertPost(post.toLikedPostEntity())
    }

    /**
     * Remove a liked post from cache (when unliked)
     */
    suspend fun removeLikedPost(postId: String) {
        likedPostDao.deleteLikedPost(postId)
    }

    /**
     * Update likes count on a liked post
     */
    suspend fun updateLikedPostLikes(postId: String, likes: Int) {
        likedPostDao.updatePostLikes(postId, likes)
    }

    /**
     * Get liked post count
     */
    suspend fun getLikedPostCount(): Int {
        return likedPostDao.getLikedPostCount()
    }

    private fun getCurrentTimeMillis(): Long {
        return Clock.System.now().toEpochMilliseconds()
    }
}