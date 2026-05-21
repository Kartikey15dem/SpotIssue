package org.example.project.profile.data.repository

import co.touchlab.kermit.Logger
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.example.project.core.database.entities.ProfileEntity
import org.example.project.core.settings.AuthSettings
import org.example.project.home.domain.models.MediaType
import org.example.project.home.domain.models.Post
import org.example.project.home.domain.models.PostLevel
import org.example.project.profile.data.local.ProfileLocalDataSource
import org.example.project.profile.data.local.mapper.Sort
import org.example.project.profile.domain.models.Profile
import org.example.project.core.data.repository.ProfileRepository
import kotlin.time.Clock

/**
 * Fake implementation of ProfileRepository that:
 * - Keeps REAL local data source and caching logic
 * - FAKES only the remote API calls (Supabase)
 * - Perfect for testing caching behavior without network dependencies
 */
class FakeProfileRepositoryImpl(
    private val localDataSource: ProfileLocalDataSource,
    private val authSettings: AuthSettings
) : ProfileRepository {

    private val logger = Logger.withTag("FakeProfileRepository")

    companion object {
        private const val CACHE_VALIDITY_MS = 5 * 60 * 1000L // 5 minutes
        private const val CURRENT_USER_ID = "current_user"
    }

    // ============ Mock Data ============

    private val mockProfile = Profile(
        imageUrl = "",
        name = "Test User",
        location = "Downtown, Central District, Test State, Test Country",
        locality = "Downtown",
        district = "Central District",
        state = "Test State",
        country = "Test Country",
        totalPosts = 5,
        acks = 12,
        postByArea = listOf(2, 1, 1, 1),
        myPosts = emptyList(),
        ackPosts = emptyList()
    )

    private val mockUserPosts = listOf(
        Post(
            id = "post_1",
            userUrl = "",
            userName = "Test User",
            timeAgo = "2 hours ago",
            postLevel = PostLevel.LOCALITY,
            location = "Downtown, Central District",
            postText = "My first issue report - testing the app functionality",
            mediaType = MediaType.IMAGE,
            mediaUrl = "",
            likes = 5,
            comments = 2
        ),
        Post(
            id = "post_2",
            userUrl = "",
            userName = "Test User",
            timeAgo = "1 day ago",
            postLevel = PostLevel.DISTRICT,
            location = "Central District",
            postText = "District-wide water shortage issue",
            mediaType = MediaType.IMAGE,
            mediaUrl = "",
            likes = 12,
            comments = 4
        )
    )

    private val mockLikedPosts = listOf(
        Post(
            id = "liked_post_1",
            userUrl = "",
            userName = "Other User",
            timeAgo = "2 days ago",
            postLevel = PostLevel.LOCALITY,
            location = "Uptown, Central District",
            postText = "Street light maintenance needed urgently",
            mediaType = MediaType.IMAGE,
            mediaUrl = "",
            likes = 20,
            comments = 8
        )
    )

    // ============ Profile Operations ============

    override suspend fun getProfile(userId: String?): Result<Profile> {
        logger.d { "FAKE: getProfile called" }

        // REAL: Try cache first using actual local data source
        val cachedProfile = localDataSource.getProfile()
        if (cachedProfile != null) {
            logger.d { "FAKE: Returning cached profile" }
            return Result.success(cachedProfile.toProfile())
        }

        // If no cache, fetch from fake remote
        return fetchAndCacheProfile(userId)
    }

    override fun getProfileFromCache(): Flow<Profile?> {
        // REAL: Use actual local data source
        return localDataSource.getProfileFlow().map { entity ->
            entity?.toProfile()
        }
    }

    override suspend fun fetchAndCacheProfile(userId: String?): Result<Profile> {
        logger.d { "FAKE: fetchAndCacheProfile called" }
        return try {
            // FAKE: Simulate network delay
            delay(800)

            // FAKE: Return mock data
            val profile = mockProfile
            logger.d { "FAKE: Fetched profile from mock API" }

            // REAL: Cache to Room DB
            val profileEntity = profile.toEntity()
            localDataSource.saveProfile(profileEntity)

            // REAL: Update sync timestamp
            authSettings.setLastProfileSync(Clock.System.now().toEpochMilliseconds())

            Result.success(profile)
        } catch (e: Exception) {
            logger.e(e) { "FAKE: Failed to fetch and cache profile" }
            Result.failure(e)
        }
    }

    override suspend fun updateProfile(profile: Profile): Result<Profile> {
        return try {
            // FAKE: Simulate network delay
            delay(1000)
            logger.d { "FAKE: Updated profile on mock API" }

            // REAL: Update cache
            localDataSource.saveProfile(profile.toEntity())

            Result.success(profile)
        } catch (e: Exception) {
            logger.e(e) { "FAKE: Failed to update profile" }
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
                userId = CURRENT_USER_ID,
                name = name,
                imageUrl = imageUrl,
                locality = locality,
                district = district,
                state = state,
                country = country,
                totalPosts = 0,
                acks = 0,
                updatedAt = Clock.System.now().toEpochMilliseconds(),
                lastSyncedAt = Clock.System.now().toEpochMilliseconds()
            )

            // REAL: Save to actual local data source
            localDataSource.saveProfile(profileEntity)

            // FAKE: Simulate Supabase sync
            delay(500)
            logger.d { "FAKE: Profile upserted to mock API" }

            Result.success(Unit)
        } catch (e: Exception) {
            logger.e(e) { "FAKE: Failed to upsert profile" }
            Result.failure(e)
        }
    }

    override suspend fun refreshProfile(userId: String?): Result<Profile> {
        logger.d { "FAKE: refreshProfile called" }
        return fetchAndCacheProfile(userId)
    }

    // ============ User Posts Operations ============

    override suspend fun getUserPosts(userId: String?): Result<List<Post>> {
        // REAL: Try cache first using actual local data source
        val cached = getUserPostsFromCache(Sort.LATEST)
        if (cached.isNotEmpty()) {
            return Result.success(cached)
        }

        // If no cache, fetch from fake remote
        return fetchAndCacheUserPosts(userId)
    }

    override suspend fun getUserPostsFromCache(sort: Sort): List<Post> {
        // REAL: Use actual local data source
        return localDataSource.getCachedUserPosts(sort)
    }

    override suspend fun fetchAndCacheUserPosts(userId: String?): Result<List<Post>> {
        logger.d { "FAKE: fetchAndCacheUserPosts called" }
        return try {
            // FAKE: Simulate network delay
            delay(600)

            // FAKE: Return mock posts
            val posts = mockUserPosts
            logger.d { "FAKE: Fetched ${posts.size} user posts from mock API" }

            // REAL: Cache to Room DB
            localDataSource.cacheUserPosts(posts)

            Result.success(posts)
        } catch (e: Exception) {
            logger.e(e) { "FAKE: Failed to fetch and cache user posts" }
            Result.failure(e)
        }
    }

    override suspend fun addPostToCache(post: Post) {
        // REAL: Use actual local data source
        localDataSource.addUserPost(post)
    }

    override suspend fun deletePostFromCache(postId: String) {
        // REAL: Use actual local data source
        localDataSource.deleteUserPost(postId)
    }

    override suspend fun updatePostLikeInCache(postId: String, isLiked: Boolean) {
        // REAL: Use actual local data source
        val currentPost = localDataSource.getCachedUserPosts(Sort.LATEST).find { it.id == postId }
        if (currentPost != null) {
            val newLikes = if (isLiked) currentPost.likes + 1 else currentPost.likes - 1
            localDataSource.updateUserPostLikeStatus(postId, newLikes, isLiked)
        }
    }

    // ============ Liked Posts Operations ============

    override suspend fun getLikedPosts(userId: String?): Result<List<Post>> {
        // REAL: Try cache first using actual local data source
        val cached = getLikedPostsFromCache(Sort.LATEST)
        if (cached.isNotEmpty()) {
            return Result.success(cached)
        }

        // If no cache, fetch from fake remote
        return fetchAndCacheLikedPosts(userId)
    }

    override suspend fun getLikedPostsFromCache(sort: Sort): List<Post> {
        // REAL: Use actual local data source
        return localDataSource.getCachedLikedPosts(sort)
    }

    override suspend fun fetchAndCacheLikedPosts(userId: String?): Result<List<Post>> {
        logger.d { "FAKE: fetchAndCacheLikedPosts called" }
        return try {
            // FAKE: Simulate network delay
            delay(700)

            // FAKE: Return mock liked posts
            val posts = mockLikedPosts
            logger.d { "FAKE: Fetched ${posts.size} liked posts from mock API" }

            // REAL: Cache to Room DB
            localDataSource.cacheLikedPosts(posts)

            Result.success(posts)
        } catch (e: Exception) {
            logger.e(e) { "FAKE: Failed to fetch and cache liked posts" }
            Result.failure(e)
        }
    }

    override suspend fun addLikedPostToCache(post: Post) {
        // REAL: Use actual local data source
        localDataSource.addLikedPost(post)
    }

    override suspend fun removeLikedPostFromCache(postId: String) {
        // REAL: Use actual local data source
        localDataSource.removeLikedPost(postId)
    }

    // ============ Cache State Management ============

    override fun isFirstLoad(): Boolean {
        // REAL: Use actual auth settings
        return authSettings.isFirstProfileLoad()
    }

    override fun markAsLoaded() {
        // REAL: Use actual auth settings
        authSettings.markProfileLoaded()
    }

    override fun shouldRefreshCache(): Boolean {
        // REAL: Use actual auth settings
        val lastSync = authSettings.getLastProfileSync()
        val currentTime = Clock.System.now().toEpochMilliseconds()
        return (currentTime - lastSync) > CACHE_VALIDITY_MS
    }

    // ============ Helper Methods ============

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
            postByArea = listOf(0, 0, 0, 0), // Default for now
            myPosts = emptyList(),
            ackPosts = emptyList()
        )
    }

    private fun Profile.toEntity(): ProfileEntity {
        return ProfileEntity(
            userId = CURRENT_USER_ID,
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
