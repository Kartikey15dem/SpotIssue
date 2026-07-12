package org.example.project.core.data.repositoryImp

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import org.example.project.core.data.repository.ProfileRepository
import org.example.project.core.database.IssueSpotDatabase
import org.example.project.core.database.entities.toProfile
import org.example.project.core.network.services.ProfileService
import org.example.project.core.model.home.Post
import org.example.project.core.data.local.ProfileLocalDataSource
import org.example.project.core.model.profile.Profile
import org.example.project.core.utils.DataState
import org.example.project.core.utils.asDataStateFlow
import org.example.project.core.utils.safeApiCall
import org.example.project.core.data.mappers.toPost
import org.example.project.core.data.mappers.toEntity
import org.example.project.core.data.mappers.toLikedPostEntity
import org.example.project.core.data.mappers.toUserPostEntity
import org.example.project.core.network.dto.UpsertProfileRequest
import org.example.project.core.datastore.UserPreferencesRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import org.example.project.core.window.WindowEngine
import org.example.project.core.window.WindowMode
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.content.PartData
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import okio.SYSTEM
import org.example.project.core.network.dto.EmailChangeRequest
import org.example.project.core.network.dto.EmailChangeVerifyRequest
import org.example.project.core.network.dto.PagedResponse
import org.example.project.core.network.dto.PostWithProfileDto
import org.example.project.core.presentation.FeedError
import org.example.project.core.presentation.FeedRefreshReason
import org.example.project.core.presentation.FeedState
import org.example.project.core.presentationcache.PresentationCache
import org.example.project.core.utils.NetworkMonitor
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ProfileRepositoryImpl(
    private val profileService: ProfileService,
    private val database: IssueSpotDatabase,
    private val localDataSource: ProfileLocalDataSource,
    private val prefRepository: UserPreferencesRepository,
    private val networkMonitor: NetworkMonitor,
) : ProfileRepository {

    private val logger = Logger.Companion.withTag("ProfileRepository")
    private val scope = CoroutineScope(Dispatchers.IO)
    private val pagingMutex = Mutex()
    private val presentationCache = PresentationCache<Post, String> { it.id }
    private val pagingStatesByKey = mutableMapOf<ProfilePostsKey, PagingState>()
    private val automaticRefreshAttemptedKeys = mutableSetOf<ProfilePostsKey>()
    private val activePostsKeyFlow = MutableStateFlow<ProfilePostsKey?>(null)
    private var activePostsKey: ProfilePostsKey?
        get() = activePostsKeyFlow.value
        set(value) { activePostsKeyFlow.value = value }
    private var pagingState = PagingState()
    private var roomJob: Job? = null
    private var networkJob: Job? = null

    private val windowEnginesByKey = mutableMapOf<ProfilePostsKey, WindowEngine<Post>>()
    private val windowState = MutableStateFlow(WindowEngine<Post>().getState())

    private fun windowEngineFor(key: ProfilePostsKey): WindowEngine<Post> {
        return windowEnginesByKey.getOrPut(key) { WindowEngine() }
    }

    private data class ProfilePostsKey(val isMine: Boolean, val sort: String)

    private data class PagingState(
        val nextPage: Int = 0,
        val hasMore: Boolean = true,
        val generation: Long = 0L,
        val lastFailedAction: RetryAction? = null,
        val lastFailedPage: Int = 0
    )

    private enum class RetryAction {
        REFRESH, LOAD_MORE
    }

    private val _profilePostsState = MutableStateFlow(FeedState())
    override val profilePostsState: StateFlow<FeedState> = _profilePostsState.asStateFlow()

    companion object {
        private const val CURRENT_USER_ID = "current_user"
    }

    override fun startProfilePosts(isMine: Boolean, sort: String) {
        val key = ProfilePostsKey(isMine = isMine, sort = sort)
        if (activePostsKey == key) return

        scope.launch {
            val cachedCount = getCachedPostCount(key)
            var shouldRefresh = false
            pagingMutex.withLock {
                networkJob?.cancel()
                saveCurrentPagingState()
                val restored = pagingStatesByKey[key] ?: PagingState()
                pagingState = restored.copy(
                    generation = restored.generation + 1,
                    lastFailedAction = null
                )
                presentationCache.clear()
                activePostsKey = key
                windowState.value = windowEngineFor(key).getState()
                shouldRefresh = key !in automaticRefreshAttemptedKeys
                _profilePostsState.value = FeedState(
                    isLoading = shouldRefresh && cachedCount == 0,
                    hasMore = pagingState.hasMore
                )
            }

            restartRoomObservation()
            if (shouldRefresh) {
                refreshProfilePosts(FeedRefreshReason.LEVEL_CHANGED)
            } else if (cachedCount == 0) {
                _profilePostsState.update { it.copy(isLoading = false) }
            }
        }
    }

    override fun stopProfilePosts() {
        networkJob?.cancel()
        roomJob?.cancel()
    }

    override fun refreshProfilePosts(reason: FeedRefreshReason) {
        val key = activePostsKey ?: return
        if ((reason == FeedRefreshReason.LEVEL_CHANGED || reason == FeedRefreshReason.NETWORK_RESTORED) &&
            !automaticRefreshAttemptedKeys.add(key)
        ) {
            return
        }

        networkJob?.cancel()
        networkJob = scope.launch {
            val cachedCount = getCachedPostCount(key)
            val requestGeneration = pagingMutex.withLock {
                pagingState = pagingState.copy(
                    nextPage = 0,
                    hasMore = true,
                    generation = pagingState.generation + 1,
                    lastFailedAction = null,
                    lastFailedPage = 0
                )
                windowState.value = windowEngineFor(key).reset()
                pagingState.generation
            }

            _profilePostsState.update {
                it.copy(
                    isLoading = cachedCount == 0,
                    isRefreshing = cachedCount > 0,
                    isAppending = false,
                    isRetrying = false,
                    hasMore = true,
                    error = null,
                    appendError = null
                )
            }

            try {
                val response = fetchProfilePage(key, 0)
                val posts = response.items.map { it.toPost() }
                val hasMore = response.nextKey != null && posts.isNotEmpty()
                pagingMutex.withLock {
                    if (requestGeneration != pagingState.generation) return@launch
                    replaceProfilePosts(key, posts)
                    pagingState = pagingState.copy(
                        nextPage = response.nextKey ?: 0,
                        hasMore = hasMore
                    )
                    saveCurrentPagingState()
                }
                _profilePostsState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isAppending = false,
                        isRetrying = false,
                        hasMore = hasMore,
                        error = null,
                        appendError = null
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val error = mapToFeedError(e)
                pagingMutex.withLock {
                    if (requestGeneration == pagingState.generation) {
                        pagingState = pagingState.copy(lastFailedAction = RetryAction.REFRESH, lastFailedPage = 0)
                        saveCurrentPagingState()
                    }
                }
                _profilePostsState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isRetrying = false,
                        error = error
                    )
                }
            }
        }
    }

    override fun loadMoreProfilePosts() {
        val key = activePostsKey ?: return
        networkJob = scope.launch {
            val (shouldFetch, requestGeneration, pageToLoad) = pagingMutex.withLock {
                val state = _profilePostsState.value
                if (state.isLoading || state.isRefreshing || state.isAppending || state.isRetrying || !pagingState.hasMore) {
                    return@withLock Triple(false, 0L, 0)
                }
                
                val currentState = windowState.value
                val items = presentationCache.items
                val anchor = if (items.size > 20) items[items.size - 20] else items.lastOrNull() // 20 is buffer
                val nextState = windowEngineFor(key).expand(anchor)
                
                if (nextState != currentState) {
                    windowState.value = nextState
                    _profilePostsState.update { it.copy(isAppending = true, appendError = null) }
                    Triple(true, pagingState.generation, pagingState.nextPage)
                } else {
                    Triple(false, 0L, 0)
                }
            }

            if (!shouldFetch) return@launch

            try {
                val response = fetchProfilePage(key, pageToLoad)
                val posts = response.items.map { it.toPost() }
                val hasMore = response.nextKey != null && posts.isNotEmpty()
                pagingMutex.withLock {
                    if (requestGeneration != pagingState.generation) return@launch
                    appendProfilePosts(key, posts)
                    pagingState = pagingState.copy(
                        nextPage = response.nextKey ?: pageToLoad,
                        hasMore = hasMore
                    )
                    saveCurrentPagingState()
                }
                _profilePostsState.update {
                    it.copy(
                        isAppending = false,
                        hasMore = hasMore,
                        appendError = null
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val error = mapToFeedError(e)
                pagingMutex.withLock {
                    if (requestGeneration == pagingState.generation) {
                        pagingState = pagingState.copy(lastFailedAction = RetryAction.LOAD_MORE, lastFailedPage = pageToLoad)
                        saveCurrentPagingState()
                    }
                }
                _profilePostsState.update {
                    it.copy(
                        isAppending = false,
                        hasMore = pagingState.hasMore,
                        appendError = error
                    )
                }
            }
        }
    }

    override fun retryProfilePosts() {
        when (pagingState.lastFailedAction) {
            RetryAction.LOAD_MORE -> loadMoreProfilePosts()
            else -> refreshProfilePosts(FeedRefreshReason.RETRY)
        }
    }

    private fun saveCurrentPagingState() {
        activePostsKey?.let { key ->
            pagingStatesByKey[key] = pagingState
        }
    }

    private fun restartRoomObservation() {
        roomJob?.cancel()
        roomJob = scope.launch {
            combine(activePostsKeyFlow, windowState) { key, window -> Pair(key, window) }
                .flatMapLatest { (key, window) ->
                    if (key == null) flowOf(emptyList())
                    else observeProfilePosts(key, window.anchor?.createdAt, window.anchor?.id, window.limit)
                }
                .collect { posts ->
                    presentationCache.update(posts)
                    _profilePostsState.update { it.copy(posts = presentationCache.items.toList()) }
                }
        }
    }

    private fun observeProfilePosts(key: ProfilePostsKey, anchorCreatedAt: Long?, anchorId: String?, limit: Int): Flow<List<Post>> {
        val flow = if (key.isMine) {
            if (anchorCreatedAt != null && anchorId != null) {
                database.userPostDao().observeAfterAnchor(key.sort, anchorCreatedAt, anchorId, limit)
            } else {
                database.userPostDao().observeNewest(key.sort, limit)
            }
        } else {
            if (anchorCreatedAt != null && anchorId != null) {
                database.likedPostDao().observeAfterAnchor(key.sort, anchorCreatedAt, anchorId, limit)
            } else {
                database.likedPostDao().observeNewest(key.sort, limit)
            }
        }
        return flow.map { posts -> posts.map { if (it is org.example.project.core.database.entities.UserPostEntity) it.toPost() else (it as org.example.project.core.database.entities.LikedPostEntity).toPost() } }
    }

    private suspend fun getCachedPostCount(key: ProfilePostsKey): Int {
        return if (key.isMine) database.userPostDao().getUserPostCount(key.sort)
        else database.likedPostDao().getLikedPostCount(key.sort)
    }

    private suspend fun fetchProfilePage(
        key: ProfilePostsKey,
        page: Int
    ): PagedResponse<PostWithProfileDto> {
        return if (key.isMine) {
            profileService.getMyPosts(page = page, limit = 20, sort = key.sort)
        } else {
            profileService.getMyLikedPosts(page = page, limit = 20, sort = key.sort)
        }
    }

    private suspend fun replaceProfilePosts(key: ProfilePostsKey, posts: List<Post>) {
        if (key.isMine) {
            database.userPostDao().deleteAllUserPosts(key.sort)
            database.userPostDao().insertPosts(posts.map { it.toUserPostEntity(sort = key.sort) })
        } else {
            database.likedPostDao().deleteAllLikedPosts(key.sort)
            database.likedPostDao().insertPosts(posts.map { it.toLikedPostEntity(sort = key.sort) })
        }
    }

    private suspend fun appendProfilePosts(key: ProfilePostsKey, posts: List<Post>) {
        if (key.isMine) {
            database.userPostDao().insertPosts(posts.map { it.toUserPostEntity(sort = key.sort) })
        } else {
            database.likedPostDao().insertPosts(posts.map { it.toLikedPostEntity(sort = key.sort) })
        }
    }

    private fun mapToFeedError(e: Throwable): FeedError {
        val msg = e.message ?: ""
        return when {
            msg.contains("401") -> FeedError.Authentication()
            msg.contains("50") -> FeedError.Server()
            msg.contains("Timeout") -> FeedError.Timeout()
            msg.contains("resolve host") || msg.contains("Failed to connect") -> FeedError.Offline()
            msg.contains("Serialization") || msg.contains("JSON") -> FeedError.Parsing()
            e is io.ktor.utils.io.errors.IOException -> FeedError.Network()
            else -> FeedError.Unknown(msg)
        }
    }





    override suspend fun refreshUserPosts(sort: String): DataState<List<Post>> =
        safeApiCall(networkMonitor) {
            val posts = profileService.getMyPosts(page = 1, limit = 100, sort = sort)
                .items
                .map { it.toPost() }
            database.userPostDao().insertPosts(posts.map { it.toUserPostEntity(sort = sort) })
            posts
        }

    override suspend fun refreshLikedPosts(sort: String): DataState<List<Post>> =
        safeApiCall(networkMonitor) {
            val posts = profileService.getMyLikedPosts(page = 1, limit = 100, sort = sort)
                .items
                .map { it.toPost() }
            database.likedPostDao().insertPosts(posts.map { it.toLikedPostEntity(sort = sort) })
            posts
        }


    override fun observeProfile(): Flow<DataState<Profile?>> = combine(
        localDataSource.getProfileFlow(),
        prefRepository.userData
    ) { entity, userData ->
        entity?.toProfile()?.copy(
            location = userData.userLocation.address.ifEmpty { "No location set" }
        )
    }.asDataStateFlow()

    override suspend fun refreshProfile(): DataState<Unit> = safeApiCall(networkMonitor) {
        val profileDto = profileService.getMyProfile()
        localDataSource.saveProfile(profileDto.toEntity())
    }

    override suspend fun updateProfile(profile: Profile, imagePath: String?): DataState<Unit> = safeApiCall(networkMonitor) {
        val request = UpsertProfileRequest(
            name = profile.name,
            email = profile.email,
            imageUrl = profile.imageUrl,
        )
        
        val multipartParts = mutableListOf<PartData>()

        multipartParts.add(
            PartData.FormItem(
                value = Json.encodeToString(request),
                dispose = {},
                partHeaders = Headers.build {
                    append(HttpHeaders.ContentDisposition, "form-data; name=\"profile\"")
                    append(HttpHeaders.ContentType, "application/json")
                }
            )
        )

        imagePath?.let { pathString ->
            val path = pathString.toPath()
            val fileName = path.name
            val bytes = FileSystem.SYSTEM.source(path).buffer().readByteArray()

            multipartParts.add(
                PartData.FileItem(
                    provider = { ByteReadChannel(bytes) },
                    dispose = {},
                    partHeaders = Headers.build {
                        append(HttpHeaders.ContentDisposition, "form-data; name=\"file\"; filename=\"$fileName\"")
                        append(HttpHeaders.ContentType, "application/octet-stream")
                    }
                )
            )
        }

        val profileDto = profileService.updateMyProfile(MultiPartFormDataContent(multipartParts))
        val entity = profileDto.toEntity()
        localDataSource.saveProfile(entity)
        database.userPostDao().updateUserInfo(ownerId = entity.userId, name = entity.name, avatar = entity.imageUrl)
        database.likedPostDao().updateUserInfo(ownerId = entity.userId, name = entity.name, avatar = entity.imageUrl)
    }

    override suspend fun requestEmailChange(newEmail: String): DataState<Unit> = safeApiCall(networkMonitor) {
        profileService.requestEmailChange(EmailChangeRequest(newEmail))
    }

    override suspend fun verifyEmailChange(
        newEmail: String,
        code: String
    ): DataState<Unit> = safeApiCall(networkMonitor) {
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
