package org.example.project.core.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import org.example.project.core.database.IssueSpotDatabase
import org.example.project.core.database.entities.CacheMetadataEntity
import org.example.project.core.database.entities.PostEntity
import org.example.project.core.database.entities.RemoteKeysEntity
import org.example.project.core.database.entities.toEntity
import org.example.project.core.network.services.HomeService
import org.example.project.core.utils.parseIsoEpochMillis
import kotlin.time.Clock
import org.example.project.core.data.local.FeedLocalDataSource
import org.example.project.core.data.mappers.toPost
import org.example.project.core.database.entities.toEntity
import org.example.project.core.model.auth.UserLocation
import org.example.project.core.model.home.PostLevel
import org.example.project.core.network.NetworkMonitor
import org.example.project.core.utils.DataState
import org.example.project.core.utils.safeApiCall

@OptIn(ExperimentalPagingApi::class)
class FeedPostsRemoteMediator(
    private val postLevel: PostLevel,
    private val userLocation: UserLocation?,
    private val homeService: HomeService,
    private val database: IssueSpotDatabase,
    private val localDataSource: FeedLocalDataSource,
    private val forceRefresh: Boolean,
    private val networkMonitor: NetworkMonitor,
) : RemoteMediator<Int, PostEntity>() {

    private val postDao = database.postDao()
    private val remoteKeysDao = database.remoteKeysDao()
    private val cacheMetadataDao = database.cacheMetadataDao()

    private val keyType = "FEED_${postLevel.name}"
    private val maxCachedPosts = 100

    override suspend fun initialize(): InitializeAction {
        if (forceRefresh) return InitializeAction.LAUNCH_INITIAL_REFRESH
        return if (localDataSource.isPostsCacheStale(postLevel)) {
            InitializeAction.LAUNCH_INITIAL_REFRESH
        } else {
            InitializeAction.SKIP_INITIAL_REFRESH
        }
    }

    override suspend fun load(loadType: LoadType, state: PagingState<Int, PostEntity>): MediatorResult {
        val page = when (loadType) {
            LoadType.REFRESH -> {
                val remoteKeys = getRemoteKeyClosestToCurrentPosition(state)
                remoteKeys?.nextKey?.minus(1) ?: 1
            }
            LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
            LoadType.APPEND -> {
                val remoteKeys = getRemoteKeyForLastItem(state)
                val nextKey = remoteKeys?.nextKey ?: return MediatorResult.Success(
                    endOfPaginationReached = true
                )
                nextKey
            }
        }

        return when (val result = safeApiCall(networkMonitor) {
            homeService.getPosts(
                level = postLevel.name,
                locality = userLocation?.locality,
                district = userLocation?.district,
                state = userLocation?.state,
                country = userLocation?.country,
                lat = userLocation?.latitude,
                lon = userLocation?.longitude,
                page = page,
                limit = state.config.pageSize,
            )
        }) {
            is DataState.Success -> {
                val response = result.data

            val posts = response.items.map { dto ->
                val post = dto.toPost()
                post.toEntity(cachedAt = parseIsoEpochMillis(dto.createdAt))
            }

            val endOfPaginationReached = response.nextKey == null || posts.isEmpty()

            if (loadType == LoadType.REFRESH) {
                remoteKeysDao.clearRemoteKeys(keyType)
                postDao.deletePostsByLevel(postLevel.name)
            }

            val keys = posts.map { post ->
                RemoteKeysEntity(
                    id = post.id,
                    prevKey = response.prevKey,
                    nextKey = response.nextKey,
                    type = keyType,
                )
            }
            remoteKeysDao.insertAll(keys)

            postDao.insertPosts(posts)
            postDao.trimPostsByLevel(postLevel.name, maxCachedPosts)
            response.activeIssuesCount?.let { count ->
                localDataSource.cacheActiveIssues(postLevel, count)
            }

            val now = Clock.System.now().toEpochMilliseconds()
            cacheMetadataDao.insertMetadata(
                CacheMetadataEntity(
                    cacheKey = CacheMetadataEntity.postsKey(postLevel.name),
                    lastFetchedAt = now,
                ),
            )

            MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
            }
            is DataState.Error -> MediatorResult.Error(result.exception)
            DataState.Loading -> MediatorResult.Error(IllegalStateException("Unexpected paging loading state"))
        }
    }

    private suspend fun getRemoteKeyForLastItem(state: PagingState<Int, PostEntity>): RemoteKeysEntity? {
        val last = state.pages.lastOrNull { it.data.isNotEmpty() }?.data?.lastOrNull() ?: return null
        return remoteKeysDao.remoteKeysId(last.id, keyType)
    }

    private suspend fun getRemoteKeyClosestToCurrentPosition(state: PagingState<Int, PostEntity>): RemoteKeysEntity? {
        val anchor = state.anchorPosition ?: return null
        val closest = state.closestItemToPosition(anchor) ?: return null
        return remoteKeysDao.remoteKeysId(closest.id, keyType)
    }
}
