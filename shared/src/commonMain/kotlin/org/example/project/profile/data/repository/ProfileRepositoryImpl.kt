package org.example.project.profile.data.repository

import co.touchlab.kermit.Logger
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import org.example.project.core.data.local.entities.ProfileEntity
import org.example.project.core.settings.AuthSettings
import org.example.project.home.domain.models.MediaType
import org.example.project.home.domain.models.Post
import org.example.project.home.domain.models.PostLevel
import org.example.project.profile.data.local.ProfileLocalDataSource
import org.example.project.profile.data.local.mapper.Sort
import org.example.project.profile.domain.repository.ProfileRepository
import kotlin.time.Clock
import org.example.project.profile.domain.models.Profile

/**
 * Implementation of ProfileRepository with caching strategy
 * 1. First load: Fetch from Supabase → Cache to Room DB
 * 2. Subsequent opens: Show cached data instantly → Optionally refresh from Supabase
 */
class ProfileRepositoryImpl(
    private val localDataSource: ProfileLocalDataSource,
    private val authSettings: AuthSettings,
    private val supabase: SupabaseClient
) : ProfileRepository {

    private val logger = Logger.withTag("ProfileRepository")

    companion object {
        private const val CACHE_VALIDITY_MS = 5 * 60 * 1000L // 5 minutes
        private const val CURRENT_USER_ID = "current_user" // TODO: Get from auth session
    }

    // ============ Supabase DTOs ============

    @Serializable
    data class ProfileDto(
        val id: String,
        val image_url: String? = null,
        val name: String,
        val location: String? = null,
        val locality: String? = null,
        val district: String? = null,
        val state: String? = null,
        val country: String? = null,
        val total_posts: Int = 0,
        val acks: Int = 0
    )

    @Serializable
    data class PostDto(
        val id: String,
        val user_id: String,
        val post_level: String,
        val location: String,
        val post_text: String,
        val media_type: String? = null,
        val media_url: String? = null,
        val likes: Int = 0,
        val comments: Int = 0,
        val created_at: String
    )

    @Serializable
    data class PostAckDto(
        val user_id: String,
        val post_id: String,
        val created_at: String
    )

    // ============ Profile Operations ============

    override fun getProfileFromCache(): Flow<Profile?> {
        return localDataSource.getProfileFlow().map { entity ->
            entity?.toProfile()
        }
    }

    override suspend fun fetchAndCacheProfile(userId: String?): Result<Profile> {
        logger.d { "fetchAndCacheProfile called" }
        return try {
            val targetUserId = userId ?: CURRENT_USER_ID

            // Fetch from Supabase profiles table
            val profileDto = supabase.from("profiles")
                .select {
                    filter {
                        eq("id", targetUserId)
                    }
                }
                .decodeSingle<ProfileDto>()

            // Convert DTO to domain model
            val profile = Profile(
                imageUrl = profileDto.image_url ?: "",
                name = profileDto.name,
                location = profileDto.location ?: "",
                locality = profileDto.locality ?: "",
                district = profileDto.district ?: "",
                state = profileDto.state ?: "",
                country = profileDto.country ?: "",
                totalPosts = profileDto.total_posts,
                acks = profileDto.acks,
                postByArea = listOf(0, 0, 0, 0), // Will be calculated from posts
                myPosts = emptyList(),
                ackPosts = emptyList()
            )

            // Cache to Room DB
            val profileEntity = profile.toEntity()
            localDataSource.saveProfile(profileEntity)

            // Update sync timestamp
            authSettings.setLastProfileSync(Clock.System.now().toEpochMilliseconds())

            logger.d { "Profile cached successfully from Supabase" }
            Result.success(profile)
        } catch (e: Exception) {
            logger.e(e) { "Failed to fetch and cache profile from Supabase" }
            Result.failure(e)
        }
    }

    override suspend fun getProfile(userId: String?): Result<Profile> {
        logger.d { "getProfile called" }

        // Try cache first
        val cachedProfile = localDataSource.getProfile()
        if (cachedProfile != null) {
            logger.d { "Returning cached profile" }
            return Result.success(cachedProfile.toProfile())
        }

        // If no cache, fetch from remote
        return fetchAndCacheProfile(userId)
    }

    override suspend fun updateProfile(profile: Profile): Result<Profile> {
        return try {
            delay(1000) // Simulate network delay
            // TODO: Call Supabase to update profile

            // Update cache
            localDataSource.saveProfile(profile.toEntity())

            Result.success(profile)
        } catch (e: Exception) {
            logger.e(e) { "Failed to update profile" }
            Result.failure(e)
        }
    }

    override suspend fun upsertProfile(
        name: String,
        imageUrl: String?,
        locality: String?,
        district: String?,
        state: String?,
        country: String?
    ): Result<Unit> {
        return try {
            val profileEntity = ProfileEntity(
                userId = "current_user",
                name = name,
                imageUrl = imageUrl,
                locality = locality,
                district = district,
                state = state,
                country = country,
                updatedAt = Clock.System.now().toEpochMilliseconds()
            )
            localDataSource.saveProfile(profileEntity)

            // TODO: Sync with Supabase
            delay(500) // Simulate network delay

            Result.success(Unit)
        } catch (e: Exception) {
            logger.e(e) { "Failed to upsert profile" }
            Result.failure(e)
        }
    }

    override suspend fun refreshProfile(userId: String?): Result<Profile> {
        logger.d { "refreshProfile called" }
        return fetchAndCacheProfile(userId)
    }

    // ============ User Posts Operations ============

    override suspend fun getUserPostsFromCache(sort: Sort): List<Post> {
        return localDataSource.getCachedUserPosts(sort)
    }

    override suspend fun fetchAndCacheUserPosts(userId: String?): Result<List<Post>> {
        logger.d { "fetchAndCacheUserPosts called" }
        return try {
            val targetUserId = userId ?: CURRENT_USER_ID

            // Fetch user's posts from Supabase
            val postDtos = supabase.from("posts")
                .select {
                    filter {
                        eq("user_id", targetUserId)
                    }
                }
                .decodeList<PostDto>()

            // Get user profile for userName and userUrl
            val profileDto = supabase.from("profiles")
                .select {
                    filter {
                        eq("id", targetUserId)
                    }
                }
                .decodeSingle<ProfileDto>()

            // Convert DTOs to domain models
            val posts = postDtos.map { dto ->
                Post(
                    id = dto.id,
                    userName = profileDto.name,
                    userUrl = profileDto.image_url ?: "",
                    postText = dto.post_text,
                    mediaType = when (dto.media_type) {
                        "image" -> MediaType.IMAGE
                        "video" -> MediaType.VIDEO
                        else -> MediaType.IMAGE
                    },
                    mediaUrl = dto.media_url ?: "",
                    location = dto.location,
                    postLevel = when (dto.post_level) {
                        "LOCALITY" -> PostLevel.LOCALITY
                        "DISTRICT" -> PostLevel.DISTRICT
                        "STATE" -> PostLevel.STATE
                        "NATIONAL" -> PostLevel.NATIONAL
                        else -> PostLevel.LOCALITY
                    },
                    likes = dto.likes,
                    comments = dto.comments,
                    timeAgo = calculateTimeAgo(dto.created_at)
                )
            }

            // Cache to Room DB
            localDataSource.cacheUserPosts(posts)

            logger.d { "Cached ${posts.size} user posts from Supabase" }
            Result.success(posts)
        } catch (e: Exception) {
            logger.e(e) { "Failed to fetch and cache user posts from Supabase" }
            Result.failure(e)
        }
    }

    override suspend fun getUserPosts(userId: String?): Result<List<Post>> {
        // Try cache first
        val cached = getUserPostsFromCache(Sort.LATEST)
        if (cached.isNotEmpty()) {
            return Result.success(cached)
        }

        // If no cache, fetch from remote
        return fetchAndCacheUserPosts(userId)
    }

    override suspend fun addPostToCache(post: Post) {
        localDataSource.addUserPost(post)
    }

    override suspend fun deletePostFromCache(postId: String) {
        localDataSource.deleteUserPost(postId)
    }

    override suspend fun updatePostLikeInCache(postId: String, isLiked: Boolean) {
        // TODO: Get current likes count and update
        val currentPost = localDataSource.getCachedUserPosts(Sort.LATEST).find { it.id == postId }
        if (currentPost != null) {
            val newLikes = if (isLiked) currentPost.likes + 1 else currentPost.likes - 1
            localDataSource.updateUserPostLikeStatus(postId, newLikes, isLiked)
        }
    }

    // ============ Liked Posts Operations ============

    override suspend fun getLikedPostsFromCache(sort: Sort): List<Post> {
        return localDataSource.getCachedLikedPosts(sort)
    }

    override suspend fun fetchAndCacheLikedPosts(userId: String?): Result<List<Post>> {
        logger.d { "fetchAndCacheLikedPosts called" }
        return try {
            val targetUserId = userId ?: CURRENT_USER_ID

            // First, get all post IDs that user has liked from post_acks table
            val postAcks = supabase.from("post_acks")
                .select {
                    filter {
                        eq("user_id", targetUserId)
                    }
                }
                .decodeList<PostAckDto>()

            if (postAcks.isEmpty()) {
                logger.d { "No liked posts found" }
                localDataSource.cacheLikedPosts(emptyList())
                return Result.success(emptyList())
            }

            // Get the post IDs
            val postIds = postAcks.map { it.post_id }

            // Fetch the actual posts
            val postDtos = supabase.from("posts")
                .select {
                    filter {
                        isIn("id", postIds)
                    }
                }
                .decodeList<PostDto>()

            // Get profiles for each post's user
            val userIds = postDtos.map { it.user_id }.distinct()
            val profiles = supabase.from("profiles")
                .select {
                    filter {
                        isIn("id", userIds)
                    }
                }
                .decodeList<ProfileDto>()

            // Create a map of userId to profile
            val profileMap = profiles.associateBy { it.id }

            // Convert DTOs to domain models
            val posts = postDtos.map { dto ->
                val userProfile = profileMap[dto.user_id]
                Post(
                    id = dto.id,
                    userName = userProfile?.name ?: "Unknown",
                    userUrl = userProfile?.image_url ?: "",
                    postText = dto.post_text,
                    mediaType = when (dto.media_type) {
                        "image" -> MediaType.IMAGE
                        "video" -> MediaType.VIDEO
                        else -> MediaType.IMAGE
                    },
                    mediaUrl = dto.media_url ?: "",
                    location = dto.location,
                    postLevel = when (dto.post_level) {
                        "LOCALITY" -> PostLevel.LOCALITY
                        "DISTRICT" -> PostLevel.DISTRICT
                        "STATE" -> PostLevel.STATE
                        "NATIONAL" -> PostLevel.NATIONAL
                        else -> PostLevel.LOCALITY
                    },
                    likes = dto.likes,
                    comments = dto.comments,
                    timeAgo = calculateTimeAgo(dto.created_at)
                )
            }

            // Cache to Room DB
            localDataSource.cacheLikedPosts(posts)

            logger.d { "Cached ${posts.size} liked posts from Supabase" }
            Result.success(posts)
        } catch (e: Exception) {
            logger.e(e) { "Failed to fetch and cache liked posts from Supabase" }
            Result.failure(e)
        }
    }

    override suspend fun getLikedPosts(userId: String?): Result<List<Post>> {
        // Try cache first
        val cached = getLikedPostsFromCache(Sort.LATEST)
        if (cached.isNotEmpty()) {
            return Result.success(cached)
        }

        // If no cache, fetch from remote
        return fetchAndCacheLikedPosts(userId)
    }

    override suspend fun addLikedPostToCache(post: Post) {
        localDataSource.addLikedPost(post)
    }

    override suspend fun removeLikedPostFromCache(postId: String) {
        localDataSource.removeLikedPost(postId)
    }

    // ============ Cache State Management ============

    override fun isFirstLoad(): Boolean {
        return authSettings.isFirstProfileLoad()
    }

    override fun markAsLoaded() {
        authSettings.markProfileLoaded()
    }

    override fun shouldRefreshCache(): Boolean {
        val lastSync = authSettings.getLastProfileSync()
        val currentTime = Clock.System.now().toEpochMilliseconds()
        return (currentTime - lastSync) > CACHE_VALIDITY_MS
    }

    // ============ Helper Methods & Fake Data ============

    /**
     * Calculate human-readable time ago from ISO timestamp
     * Example: "2024-01-07T12:00:00Z" → "2 hours ago"
     */
    private fun calculateTimeAgo(createdAt: String): String {
        return try {
            // Parse ISO timestamp and calculate difference
            // For now, return a simple format
            // TODO: Implement proper timestamp parsing with kotlinx-datetime
            "1 hour ago"
        } catch (e: Exception) {
            logger.e(e) { "Failed to parse timestamp: $createdAt" }
            "Recently"
        }
    }

    private fun ProfileEntity.toProfile(): Profile {
        return Profile(
            imageUrl = imageUrl ?: "",
            name = name,
            location = "$locality, $district, $state, $country",
            locality = locality ?: "",
            district = district ?: "",
            state = state ?: "",
            country = country ?: "",
            totalPosts = totalPosts,
            acks = acks,
            postByArea = listOf(0, 0, 0, 0), // TODO: Calculate from posts
            myPosts = emptyList(), // Loaded separately
            ackPosts = emptyList() // Loaded separately
        )
    }

    private fun Profile.toEntity(): ProfileEntity {
        return ProfileEntity(
            userId = "current_user",
            name = name,
            imageUrl = imageUrl,
            locality = locality,
            district = district,
            state = state,
            country = country,
            totalPosts = totalPosts,
            acks = acks,
            updatedAt = Clock.System.now().toEpochMilliseconds(),
            lastSyncedAt = Clock.System.now().toEpochMilliseconds()
        )
    }
}

