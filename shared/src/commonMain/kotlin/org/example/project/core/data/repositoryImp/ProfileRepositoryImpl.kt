package org.example.project.core.data.repositoryImp

import co.touchlab.kermit.Logger
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import androidx.paging.ExperimentalPagingApi
import org.example.project.core.data.repository.ProfileRepository
import org.example.project.core.database.IssueSpotDatabase
import org.example.project.core.database.entities.toProfile
import org.example.project.core.network.services.ProfileService
import org.example.project.core.model.home.Post
import org.example.project.core.data.local.ProfileLocalDataSource
import org.example.project.core.model.profile.Profile
import org.example.project.core.utils.DataState
import org.example.project.core.utils.safeApiCall
import org.example.project.core.data.mappers.toPost
import org.example.project.core.data.mappers.toEntity
import org.example.project.core.network.dto.UpsertProfileRequest
import org.example.project.core.data.paging.ProfileLikedPostsRemoteMediator
import org.example.project.core.data.paging.ProfileUserPostsRemoteMediator
import org.example.project.core.datastore.UserPreferencesRepository
import kotlinx.coroutines.flow.combine

class ProfileRepositoryImpl(
    private val profileService: ProfileService,
    private val database: IssueSpotDatabase,
    private val localDataSource: ProfileLocalDataSource,
    private val prefRepository: UserPreferencesRepository
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
                sort = sort
            ),
            pagingSourceFactory = { 
                when (sort.toUpperCase()) {
                    "OLDEST" -> database.userPostDao().pagingSourceOldest(CURRENT_USER_ID)
                    "POPULAR" -> database.userPostDao().pagingSourcePopular(CURRENT_USER_ID)
                    else -> database.userPostDao().pagingSource(CURRENT_USER_ID)
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
                sort = sort
            ),
            pagingSourceFactory = { 
                when (sort.toUpperCase()) {
                    "OLDEST" -> database.likedPostDao().pagingSourceOldest(CURRENT_USER_ID)
                    "POPULAR" -> database.likedPostDao().pagingSourcePopular(CURRENT_USER_ID)
                    else -> database.likedPostDao().pagingSource(CURRENT_USER_ID)
                }
            },
        ).flow.map { pagingData ->
            pagingData.map { entity -> entity.toPost() }
        }
    }

    override fun observeProfile(): Flow<DataState<Profile?>> = combine(
        localDataSource.getProfileFlow(),
        prefRepository.userData
    ) { entity, userData ->
        if (entity != null) {
            val domainProfile = entity.toProfile().copy(
                location = userData.userLocation?.address ?: "No location set"
            )
            DataState.Success(domainProfile)
        } else {
            DataState.Error(Exception("Profile not found"))
        }
    }.onStart { emit(DataState.Loading) }

    override suspend fun refreshProfile(): DataState<Unit> = safeApiCall {
        val profileDto = profileService.getMyProfile()
        localDataSource.saveProfile(profileDto.toEntity())
    }

    override suspend fun updateProfile(profile: Profile): DataState<Unit> = safeApiCall {
        val request = UpsertProfileRequest(
            name = profile.name,
            email = profile.email,
            imageUrl = profile.imageUrl,
        )
        val profileDto = profileService.updateMyProfile(request)
        localDataSource.saveProfile(profileDto.toEntity())
    }
}
