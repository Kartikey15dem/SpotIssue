package org.example.project.core.data.repositoryImp

import co.touchlab.kermit.Logger
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.content.PartData
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM
import okio.buffer
import org.example.project.core.data.local.ProfileLocalDataSource
import org.example.project.core.data.mappers.toEntity
import org.example.project.core.data.mappers.toLikedPostEntity
import org.example.project.core.data.mappers.toPost
import org.example.project.core.data.mappers.toUserPostEntity
import org.example.project.core.data.repository.ProfileRepository
import org.example.project.core.database.IssueSpotDatabase
import org.example.project.core.database.entities.LikedPostEntity
import org.example.project.core.database.entities.UserPostEntity
import org.example.project.core.database.entities.toProfile
import org.example.project.core.datastore.UserPreferencesRepository
import org.example.project.core.model.home.Post
import org.example.project.core.model.profile.Profile
import org.example.project.core.network.dto.EmailChangeRequest
import org.example.project.core.network.dto.EmailChangeVerifyRequest
import org.example.project.core.network.dto.UpsertProfileRequest
import org.example.project.core.network.services.ProfileService
import org.example.project.core.paging.OfflinePager
import org.example.project.core.paging.PagingResult
import org.example.project.core.presentation.FeedRefreshReason
import org.example.project.core.presentation.FeedState
import org.example.project.core.utils.DataState
import org.example.project.core.utils.NetworkMonitor
import org.example.project.core.utils.asDataStateFlow
import org.example.project.core.utils.safeApiCall
import kotlin.time.Clock

class ProfileRepositoryImpl(
    private val profileService: ProfileService,
    private val database: IssueSpotDatabase,
    private val localDataSource: ProfileLocalDataSource,
    private val prefRepository: UserPreferencesRepository,
    private val networkMonitor: NetworkMonitor,
) : ProfileRepository {
    private val logger = Logger.withTag("ProfileRepository")
    private val scope = CoroutineScope(Dispatchers.IO)
    private var isOnline = true

    // ---------------------------------------------------------
    // THE PROFILE PAGER
    // ---------------------------------------------------------

    private data class ProfilePostsKey(
        val isMine: Boolean,
        val sort: String,
    )

    // We replace 300 lines of complex paging logic with this OfflinePager engine
    private val profilePager =
        OfflinePager<ProfilePostsKey>(
            scope = scope,
            fetchFromNetwork = { key, page ->
                val response =
                    if (key.isMine) {
                        profileService.getMyPosts(page = page, limit = 20, sort = key.sort.lowercase())
                    } else {
                        profileService.getMyLikedPosts(page = page, limit = 20, sort = key.sort.lowercase())
                    }
                val posts = response.items.map { it.toPost() }
                PagingResult(posts, response.nextKey, response.nextKey != null && posts.isNotEmpty())
            },
            saveToDatabase = { key, posts, isRefresh ->
                val baseTime = Clock.System.now().toEpochMilliseconds()
                if (isRefresh) {
                    if (key.isMine) {
                        database.userPostDao().deleteAllUserPosts()
                        database.userPostDao().insertPosts(
                            posts.mapIndexed {
                                    index,
                                    post,
                                ->
                                post.toUserPostEntity(cachedAt = baseTime - index)
                            },
                        )
                    } else {
                        database.likedPostDao().deleteAllLikedPosts()
                        database.likedPostDao().insertPosts(
                            posts.mapIndexed {
                                    index,
                                    post,
                                ->
                                post.toLikedPostEntity(cachedAt = baseTime - index)
                            },
                        )
                    }
                } else {
                    val minCachedAt =
                        if (key.isMine) {
                            database.userPostDao().getMinCachedAt() ?: baseTime
                        } else {
                            database.likedPostDao().getMinCachedAt() ?: baseTime
                        }
                    if (key.isMine) {
                        database.userPostDao().insertPosts(
                            posts.mapIndexed { index, post ->
                                post.toUserPostEntity(
                                    cachedAt =
                                        minCachedAt - 1 - index,
                                )
                            },
                        )
                    } else {
                        database.likedPostDao().insertPosts(
                            posts.mapIndexed { index, post ->
                                post.toLikedPostEntity(
                                    cachedAt =
                                        minCachedAt - 1 - index,
                                )
                            },
                        )
                    }
                }
            },
            observeDatabase = { key, anchorCachedAt, anchorId, limit ->
                val flow =
                    if (key.isMine) {
                        if (anchorCachedAt != null && anchorId != null) {
                            database.userPostDao().observeAfterAnchor(key.sort, anchorCachedAt, anchorId, limit)
                        } else {
                            database.userPostDao().observeNewest(key.sort, limit)
                        }
                    } else {
                        if (anchorCachedAt != null && anchorId != null) {
                            database.likedPostDao().observeAfterAnchor(key.sort, anchorCachedAt, anchorId, limit)
                        } else {
                            database.likedPostDao().observeNewest(key.sort, limit)
                        }
                    }
                flow.map { posts -> posts.map { if (it is UserPostEntity) it.toPost() else (it as LikedPostEntity).toPost() } }
            },
            getCachedCount = { key ->
                if (key.isMine) {
                    database.userPostDao().getUserPostCount()
                } else {
                    database.likedPostDao().getLikedPostCount()
                }
            },
            isOnlineProvider = { isOnline },
        )

    override val profilePostsState: StateFlow<FeedState> = profilePager.uiState

    init {
        scope.launch {
            networkMonitor.isOnline.collect { online ->
                isOnline = online
                profilePager.updateOnlineStatus(online)
            }
        }
    }

    // ---------------------------------------------------------
    // PROFILE PAGINATION API
    // ---------------------------------------------------------

    override fun startProfilePosts(
        isMine: Boolean,
        sort: String,
    ) = profilePager.start(ProfilePostsKey(isMine, sort))

    override fun stopProfilePosts() = profilePager.stop()

    override fun clearRefreshState() = profilePager.clear()

    override fun refreshProfilePosts(reason: FeedRefreshReason) = profilePager.refresh(reason)

    override fun loadMoreProfilePosts() = profilePager.loadMore()

    override fun retryProfilePosts() = profilePager.retry()

    // ---------------------------------------------------------
    // PROFILE MANAGEMENT (Non-Pagination)
    // ---------------------------------------------------------

    override suspend fun refreshUserPosts(sort: String): DataState<List<Post>> =
        safeApiCall(networkMonitor) {
            val posts = profileService.getMyPosts(page = 1, limit = 100, sort = sort.lowercase()).items.map { it.toPost() }
            database.userPostDao().insertPosts(posts.map { it.toUserPostEntity() })
            posts
        }

    override suspend fun refreshLikedPosts(sort: String): DataState<List<Post>> =
        safeApiCall(networkMonitor) {
            val posts = profileService.getMyLikedPosts(page = 1, limit = 100, sort = sort.lowercase()).items.map { it.toPost() }
            database.likedPostDao().insertPosts(posts.map { it.toLikedPostEntity() })
            posts
        }

    override fun observeProfile(): Flow<DataState<Profile?>> =
        combine(
            localDataSource.getProfileFlow(),
            prefRepository.userData,
        ) { entity, userData ->
            entity?.toProfile()?.copy(
                location = userData.userLocation.address.ifEmpty { "No location set" },
            )
        }.asDataStateFlow()

    override suspend fun refreshProfile(): DataState<Unit> =
        safeApiCall(networkMonitor) {
            val profileDto = profileService.getMyProfile()
            localDataSource.saveProfile(profileDto.toEntity())
        }

    override suspend fun updateProfile(
        profile: Profile,
        imagePath: String?,
    ): DataState<Unit> =
        safeApiCall(networkMonitor) {
            val request =
                UpsertProfileRequest(
                    name = profile.name,
                    email = profile.email,
                    imageUrl = profile.imageUrl,
                )
            val multipartParts = mutableListOf<PartData>()
            multipartParts.add(
                PartData.FormItem(
                    value = Json.encodeToString(request),
                    dispose = {},
                    partHeaders =
                        Headers.build {
                            append(HttpHeaders.ContentDisposition, "form-data; name=\"profile\"")
                            append(HttpHeaders.ContentType, "application/json")
                        },
                ),
            )
            imagePath?.let { pathString ->
                val path = pathString.toPath()
                val fileName = path.name
                val bytes =
                    FileSystem.SYSTEM
                        .source(path)
                        .buffer()
                        .readByteArray()
                multipartParts.add(
                    PartData.FileItem(
                        provider = { ByteReadChannel(bytes) },
                        dispose = {},
                        partHeaders =
                            Headers.build {
                                append(HttpHeaders.ContentDisposition, "form-data; name=\"file\"; filename=\"$fileName\"")
                                append(HttpHeaders.ContentType, "application/octet-stream")
                            },
                    ),
                )
            }
            val profileDto = profileService.updateMyProfile(MultiPartFormDataContent(multipartParts))
            val entity = profileDto.toEntity()
            localDataSource.saveProfile(entity)
            database.userPostDao().updateUserInfo(ownerId = entity.userId, name = entity.name, avatar = entity.imageUrl)
            database.likedPostDao().updateUserInfo(ownerId = entity.userId, name = entity.name, avatar = entity.imageUrl)
        }

    override suspend fun requestEmailChange(newEmail: String): DataState<Unit> =
        safeApiCall(networkMonitor) {
            profileService.requestEmailChange(EmailChangeRequest(newEmail))
        }

    override suspend fun verifyEmailChange(
        newEmail: String,
        code: String,
    ): DataState<Unit> =
        safeApiCall(networkMonitor) {
            profileService.verifyEmailChange(EmailChangeVerifyRequest(newEmail, code))
            val profileDto = profileService.getMyProfile()
            val entity = profileDto.toEntity()
            localDataSource.saveProfile(entity)
            database.userPostDao().updateUserInfo(ownerId = entity.userId, name = entity.name, avatar = entity.imageUrl)
            database.likedPostDao().updateUserInfo(ownerId = entity.userId, name = entity.name, avatar = entity.imageUrl)
            Unit
        }

    override suspend fun logOut() {
        prefRepository.logOut()
        withContext(Dispatchers.IO) {
            database.userPostDao().clearAll()
            database.likedPostDao().clearAll()
            localDataSource.clearProfile()
        }
    }
}
