package org.example.project.core.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import org.example.project.core.database.IssueSpotDatabase
import org.example.project.core.database.entities.LikedPostEntity
import org.example.project.core.database.entities.RemoteKeysEntity
import org.example.project.core.network.services.ProfileService
import org.example.project.core.data.mappers.toPost
import org.example.project.core.data.local.ProfileLocalDataSource
import org.example.project.core.data.mappers.toLikedPostEntity
import org.example.project.core.network.NetworkMonitor
import org.example.project.core.database.dao.MediatorTransactionDao
import org.example.project.core.utils.DataState
import org.example.project.core.utils.safeApiCall

@OptIn(ExperimentalPagingApi::class)
class ProfileLikedPostsRemoteMediator(
    private val profileService: ProfileService,
    private val database: IssueSpotDatabase,
    private val localDataSource: ProfileLocalDataSource,
    private val sort: String,
    private val networkMonitor: NetworkMonitor,
) : RemoteMediator<Int, LikedPostEntity>() {

    private val remoteKeysDao = database.remoteKeysDao()
    private val likedPostDao = database.likedPostDao()

    private val targetUserId = "current_user"
    private val keyType = "LIKED_POSTS_$targetUserId"
    private val maxCachedPosts = 100

    override suspend fun initialize(): InitializeAction {
        return InitializeAction.LAUNCH_INITIAL_REFRESH
    }

    override suspend fun load(loadType: LoadType, state: PagingState<Int, LikedPostEntity>): MediatorResult {
        val page = when (loadType) {
            LoadType.REFRESH -> 0
            LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
            LoadType.APPEND -> {
                val remoteKeys = getRemoteKeyForLastItem(state)
                val nextKey = remoteKeys?.nextKey
                if (nextKey == null) return MediatorResult.Success(endOfPaginationReached = true)
                nextKey
            }
        }

        return when (val result = safeApiCall(networkMonitor) {
            profileService.getMyLikedPosts(
                page = page,
                limit = state.config.pageSize,
                sort = sort
            )
        }) {
            is DataState.Success -> {
                val response = result.data

            val entities = response.items.map { dto ->
                val post = dto.toPost()
                post.toLikedPostEntity()
            }

            val endOfPaginationReached = response.nextKey == null || entities.isEmpty()

            val remoteKeys = entities.map { e ->
                RemoteKeysEntity(
                    id = e.id,
                    prevKey = response.prevKey,
                    nextKey = response.nextKey,
                    type = keyType,
                )
            }

            if (loadType == LoadType.REFRESH) {
                database.mediatorTransactionDao().refreshLikedPosts(
                    likedPostDao = likedPostDao,
                    remoteKeysDao = remoteKeysDao,
                    keyType = keyType,
                    posts = entities,
                    remoteKeys = remoteKeys
                )
            } else {
                database.mediatorTransactionDao().appendLikedPage(
                    likedPostDao = likedPostDao,
                    remoteKeysDao = remoteKeysDao,
                    posts = entities,
                    remoteKeys = remoteKeys,
                    maxCachedPosts = maxCachedPosts
                )
            }

            MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
            }
            is DataState.Error -> MediatorResult.Error(result.exception)
            DataState.Loading -> MediatorResult.Error(IllegalStateException("Unexpected paging loading state"))
        }
    }

    private suspend fun getRemoteKeyForLastItem(state: PagingState<Int, LikedPostEntity>): RemoteKeysEntity? {
        val last = state.pages.lastOrNull { it.data.isNotEmpty() }?.data?.lastOrNull() ?: return null
        return remoteKeysDao.remoteKeysId(last.id, keyType)
    }

    private suspend fun getRemoteKeyClosestToCurrentPosition(state: PagingState<Int, LikedPostEntity>): RemoteKeysEntity? {
        val anchor = state.anchorPosition ?: return null
        val closest = state.closestItemToPosition(anchor) ?: return null
        return remoteKeysDao.remoteKeysId(closest.id, keyType)
    }
}
