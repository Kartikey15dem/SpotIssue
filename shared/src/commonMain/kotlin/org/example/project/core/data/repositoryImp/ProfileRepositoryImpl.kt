package org.example.project.core.data.repositoryImp

import co.touchlab.kermit.Logger
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.paging.ExperimentalPagingApi
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
import org.example.project.core.data.paging.ProfileLikedPostsRemoteMediator
import org.example.project.core.data.paging.ProfileUserPostsRemoteMediator
import org.example.project.core.datastore.UserPreferencesRepository
import kotlinx.coroutines.flow.combine
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
import org.example.project.core.utils.NetworkMonitor

class ProfileRepositoryImpl(
    private val profileService: ProfileService,
    private val database: IssueSpotDatabase,
    private val localDataSource: ProfileLocalDataSource,
    private val prefRepository: UserPreferencesRepository,
    private val networkMonitor: NetworkMonitor,
) : ProfileRepository {

    private val logger = Logger.Companion.withTag("ProfileRepository")

    companion object {
        private const val CURRENT_USER_ID = "current_user"
    }

    @OptIn(ExperimentalPagingApi::class, ExperimentalCoroutinesApi::class)
    override fun getPagedUserPosts(sort: String): Flow<PagingData<Post>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            remoteMediator = ProfileUserPostsRemoteMediator(
                profileService = profileService,
                database = database,
                localDataSource = localDataSource,
                sort = sort,
                networkMonitor = networkMonitor,
            ),
            pagingSourceFactory = { 
                when (sort.uppercase()) {
                    "OLDEST" -> database.userPostDao().pagingSourceOldest()
                    "POPULAR" -> database.userPostDao().pagingSourcePopular()
                    else -> database.userPostDao().pagingSource()
                }
            },
        ).flow.map { pagingData ->
            pagingData.map { entity -> entity.toPost() }
        }
    }

    @OptIn(ExperimentalPagingApi::class, ExperimentalCoroutinesApi::class)
    override fun getPagedLikedPosts(sort: String): Flow<PagingData<Post>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            remoteMediator = ProfileLikedPostsRemoteMediator(
                profileService = profileService,
                database = database,
                localDataSource = localDataSource,
                sort = sort,
                networkMonitor = networkMonitor,
            ),
            pagingSourceFactory = { 
                when (sort.uppercase()) {
                    "OLDEST" -> database.likedPostDao().pagingSourceOldest()
                    "POPULAR" -> database.likedPostDao().pagingSourcePopular()
                    else -> database.likedPostDao().pagingSource()
                }
            },
        ).flow.map { pagingData ->
            pagingData.map { entity -> entity.toPost() }
        }
    }

    override fun observeUserPosts(sort: String): Flow<List<Post>> {
        val flow = when (sort.uppercase()) {
            "OLDEST" -> database.userPostDao().observeUserPostsOldest()
            "POPULAR" -> database.userPostDao().observeUserPostsPopular()
            else -> database.userPostDao().observeUserPosts()
        }
        return flow.map { posts -> posts.map { it.toPost() } }
    }

    override fun observeLikedPosts(sort: String): Flow<List<Post>> {
        val flow = when (sort.uppercase()) {
            "OLDEST" -> database.likedPostDao().observeLikedPostsOldest()
            "POPULAR" -> database.likedPostDao().observeLikedPostsPopular()
            else -> database.likedPostDao().observeLikedPosts()
        }
        return flow.map { posts -> posts.map { it.toPost() } }
    }

    override suspend fun refreshUserPosts(sort: String): DataState<List<Post>> =
        safeApiCall(networkMonitor) {
            val posts = profileService.getMyPosts(page = 1, limit = 100, sort = sort)
                .items
                .map { it.toPost() }
            database.userPostDao().insertPosts(posts.map { it.toUserPostEntity() })
            posts
        }

    override suspend fun refreshLikedPosts(sort: String): DataState<List<Post>> =
        safeApiCall(networkMonitor) {
            val posts = profileService.getMyLikedPosts(page = 1, limit = 100, sort = sort)
                .items
                .map { it.toPost() }
            database.likedPostDao().insertPosts(posts.map { it.toLikedPostEntity() })
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
            database.userPostDao().deleteAllUserPosts()
            database.likedPostDao().deleteAllLikedPosts()
            localDataSource.clearProfile()
        }
    }
}
