package org.example.project.core.data.repositoryImp

import co.touchlab.kermit.Logger
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import org.example.project.core.data.repository.ProfileRepository
import org.example.project.core.database.IssueSpotDatabase
import org.example.project.core.database.entities.ProfileEntity
import org.example.project.core.database.entities.toProfile
import org.example.project.core.database.entities.toEntity
import org.example.project.core.network.services.ProfileService
import org.example.project.home.domain.models.Post
import org.example.project.profile.data.local.ProfileLocalDataSource
import org.example.project.profile.domain.models.Profile
import org.example.project.core.utils.DataState
import org.example.project.core.utils.safeApiCall
import kotlin.time.Clock
import org.example.project.profile.data.paging.ProfilePostsPagingSource

class ProfileRepositoryImpl(
    private val profileService: ProfileService,
    private val database: IssueSpotDatabase,
    private val localDataSource: ProfileLocalDataSource,
) : ProfileRepository {

    private val logger = Logger.Companion.withTag("ProfileRepository")

    companion object {
        private const val CURRENT_USER_ID = "current_user"
    }

    override fun getPagedUserPosts(userId: String?): Flow<PagingData<Post>> {
        val targetUserId = userId ?: CURRENT_USER_ID
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = {
                ProfilePostsPagingSource(
                    profileService = profileService,
                    userId = targetUserId,
                    kind = ProfilePostsPagingSource.Kind.USER_POSTS,
                )
            },
        ).flow
    }

    override fun getPagedLikedPosts(userId: String?): Flow<PagingData<Post>> {
        val targetUserId = userId ?: CURRENT_USER_ID
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = {
                ProfilePostsPagingSource(
                    profileService = profileService,
                    userId = targetUserId,
                    kind = ProfilePostsPagingSource.Kind.LIKED_POSTS,
                )
            },
        ).flow
    }

    override fun observeProfile(userId: String?): Flow<DataState<Profile>> = flow {
        val targetUserId = userId ?: CURRENT_USER_ID
        val dbFlow = localDataSource.getProfileFlow()
            .map { entity ->
                if (entity != null) {
                    DataState.Success(entity.toProfile())
                } else {
                    DataState.Loading // Or a custom Empty state
                }
            }

        emitAll(dbFlow)
    }.onStart { emit(DataState.Loading) }

    override suspend fun refreshProfile(userId: String?): DataState<Unit> = safeApiCall {
        val targetUserId = userId ?: CURRENT_USER_ID
        // Assume profile data is fetched. Mocking for now as ProfileService doesn't have it.
        // val profileDto = profileService.getProfile(targetUserId)
        // localDataSource.saveProfile(profileDto.toEntity())
        
        // Fake update to DB to trigger flow
        val current = localDataSource.getProfile()
            ?: ProfileEntity(userId = targetUserId, name = "User")
        localDataSource.saveProfile(current.copy(updatedAt = Clock.System.now().toEpochMilliseconds()))
        Unit
    }

    override suspend fun updateProfile(profile: Profile): DataState<Unit> = safeApiCall {
        localDataSource.saveProfile(
            profile
                .toEntity(userId = CURRENT_USER_ID)
                .copy(updatedAt = Clock.System.now().toEpochMilliseconds()),
        )
        // TODO: Sync with remote
        Unit
    }
}
