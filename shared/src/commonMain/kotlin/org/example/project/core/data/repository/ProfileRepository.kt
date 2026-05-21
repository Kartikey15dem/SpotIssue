package org.example.project.core.data.repository

import kotlinx.coroutines.flow.Flow
import org.example.project.home.domain.models.Post
import org.example.project.profile.data.local.mapper.Sort
import org.example.project.profile.domain.models.Profile

/**
 * Profile feature repository interface
 * Defines data operations for the profile feature with caching strategy
 */
interface ProfileRepository {
    // ============ Profile Operations ============

    /**
     * Get user profile by ID
     * @param userId User ID, null for current user
     */
    suspend fun getProfile(userId: String? = null): Result<Profile>

    /**
     * Get profile from cache as Flow (real-time updates)
     */
    fun getProfileFromCache(): Flow<Profile?>

    /**
     * Fetch profile from Supabase and cache it
     */
    suspend fun fetchAndCacheProfile(userId: String? = null): Result<Profile>

    /**
     * Update user profile
     */
    suspend fun updateProfile(profile: Profile): Result<Profile>

    /**
     * Upsert (insert or update) user profile
     * This will create a new profile if it doesn't exist, or update if it does
     */
    suspend fun upsertProfile(
        name: String,
        imageUrl: String? = null,
        locality: String? = null,
        district: String? = null,
        state: String? = null,
        country: String? = null
    ): Result<Unit>

    /**
     * Refresh profile data from remote
     */
    suspend fun refreshProfile(userId: String? = null): Result<Profile>

    // ============ User Posts Operations ============

    /**
     * Get posts created by a user
     * @param userId User ID, null for current user
     */
    suspend fun getUserPosts(userId: String? = null): Result<List<Post>>

    /**
     * Get user posts from cache (sorted)
     */
    suspend fun getUserPostsFromCache(sort: Sort): List<Post>

    /**
     * Fetch user posts from Supabase and cache them
     */
    suspend fun fetchAndCacheUserPosts(userId: String? = null): Result<List<Post>>

    /**
     * Add a new post to user posts cache
     */
    suspend fun addPostToCache(post: Post)

    /**
     * Delete post from user posts cache
     */
    suspend fun deletePostFromCache(postId: String)

    /**
     * Update post like status in cache
     */
    suspend fun updatePostLikeInCache(postId: String, isLiked: Boolean)

    // ============ Liked Posts Operations ============

    /**
     * Get posts liked by a user
     * @param userId User ID, null for current user
     */
    suspend fun getLikedPosts(userId: String? = null): Result<List<Post>>

    /**
     * Get liked posts from cache (sorted)
     */
    suspend fun getLikedPostsFromCache(sort: Sort): List<Post>

    /**
     * Fetch liked posts from Supabase and cache them
     */
    suspend fun fetchAndCacheLikedPosts(userId: String? = null): Result<List<Post>>

    /**
     * Add post to liked posts cache
     */
    suspend fun addLikedPostToCache(post: Post)

    /**
     * Remove post from liked posts cache
     */
    suspend fun removeLikedPostFromCache(postId: String)

    // ============ Cache State Management ============

    /**
     * Check if this is the first load (app just opened)
     */
    fun isFirstLoad(): Boolean

    /**
     * Mark profile as loaded
     */
    fun markAsLoaded()

    /**
     * Check if cache needs refresh (based on last sync time)
     */
    fun shouldRefreshCache(): Boolean
}