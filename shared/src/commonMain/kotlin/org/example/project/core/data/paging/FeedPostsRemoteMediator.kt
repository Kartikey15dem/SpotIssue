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
import org.example.project.core.data.local.FeedLocalDataSource
import org.example.project.core.data.mappers.toPost
import org.example.project.core.model.auth.UserLocation
import org.example.project.core.model.home.PostLevel
import org.example.project.core.utils.NetworkMonitor
import org.example.project.core.utils.DataState
import org.example.project.core.utils.safeApiCall

private const val DB_TRACE = "[DB_TRACE]"

@OptIn(ExperimentalPagingApi::class)
/**
 * ===================================================================================
 * SECTION: OFFLINE-FIRST PAGING ARCHITECTURE
 * ===================================================================================
 * This RemoteMediator is the core of the offline-first experience for the Home feed.
 * It sits between the local Room database (cache) and the Ktor network service.
 * 
 * Logic Flow:
 * 1. Checks `initialize()` to see if the cache is stale or a forced refresh is requested.
 * 2. On `load()`, it determines the correct pagination key (page number).
 * 3. Fetches data from the network.
 * 4. Uses `MediatorTransactionDao` to atomically clear old data and insert new data into the 
 *    local database. This atomicity prevents UI flickering (empty screens) during refreshes.
 * 5. The UI observes the local database directly via PagingSource, ensuring instant loads
 *    on subsequent app launches.
 */
class FeedPostsRemoteMediator(
    private val postLevel: PostLevel,
    private val userLocation: UserLocation,
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
    private val maxCachedPosts = 1000
    override suspend fun initialize(): InitializeAction {
        val isStale = localDataSource.isPostsCacheStale(postLevel)
        return if (forceRefresh || isStale) {
            InitializeAction.LAUNCH_INITIAL_REFRESH
        } else {
            InitializeAction.SKIP_INITIAL_REFRESH
        }
    }

    override suspend fun load(loadType: LoadType, state: PagingState<Int, PostEntity>): MediatorResult {
        val page = when (loadType) {
            LoadType.REFRESH -> {
                val remoteKeys = getRemoteKeyClosestToCurrentPosition(state)
                remoteKeys?.nextKey?.minus(1) ?: 0
            }
            LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
            LoadType.APPEND -> {
                val remoteKeys = getRemoteKeyForLastItem(state)
                remoteKeys?.nextKey ?: return MediatorResult.Success(
                    endOfPaginationReached = true
                )
            }
        }

        return when (val result = safeApiCall(networkMonitor) {
            homeService.getPosts(
                level = postLevel.name,
                locality = userLocation.locality,
                district = userLocation.district,
                state = userLocation.state,
                country = userLocation.country,
                lat = userLocation.latitude,
                lon = userLocation.longitude,
                page = page,
                limit = state.config.pageSize,
            )
        }) {
            is DataState.Success -> {
                val response = result.data
                val posts = response.items.map { dto ->
                    val post = dto.toPost()
                    // Use a stable local timestamp for sorting. 
                    // We base it on network createdAt but store it in a field that isn't updated by 'like' actions.
                    // 10000000000000L is a far-future timestamp (Year 2286) to ensure descending sort works.
                    val networkTime = parseIsoEpochMillis(dto.createdAt)
                    post.toEntity(cachedAt = networkTime)
                }

                val endOfPaginationReached = response.nextKey == null || posts.isEmpty()

                val keys = posts.map { post ->
                    RemoteKeysEntity(
                        id = post.id,
                        prevKey = response.prevKey,
                        nextKey = response.nextKey,
                        type = keyType,
                    )
                }

                println("""
$DB_TRACE MEDIATOR START
$DB_TRACE loadType=$loadType
$DB_TRACE page=$page
$DB_TRACE returnedPosts=${posts.size}
$DB_TRACE first=${posts.firstOrNull()?.id}
$DB_TRACE last=${posts.lastOrNull()?.id}
$DB_TRACE nextKey=${response.nextKey}
$DB_TRACE prevKey=${response.prevKey}
$DB_TRACE time=${kotlin.time.Clock.System.now().toEpochMilliseconds()}
$DB_TRACE =========================
""")

                if (loadType == LoadType.REFRESH) {
                    database.mediatorTransactionDao().refreshFeed(
                        postDao = postDao,
                        remoteKeysDao = remoteKeysDao,
                        level = postLevel.name,
                        keyType = keyType,
                        posts = posts,
                        remoteKeys = keys
                    )
                    
                    val afterRefresh = postDao.getPostsByLevel(postLevel.name).take(30)
                    println("[DATABASE_ORDER] AFTER REFRESH | size=${afterRefresh.size}")
                    afterRefresh.forEachIndexed { index, post ->
                        println("[DATABASE_ORDER] $index | ${post.id} | ${post.cachedAt}")
                    }
                } else {
                    database.mediatorTransactionDao().appendPage(
                        postDao = postDao,
                        remoteKeysDao = remoteKeysDao,
                        posts = posts,
                        remoteKeys = keys,
                        level = postLevel.name,
                        keyType = keyType,
                        maxCachedPosts = maxCachedPosts
                    )
                    
                    val afterAppend = postDao.getPostsByLevel(postLevel.name).take(30)
                    println("[DATABASE_ORDER] AFTER APPEND | size=${afterAppend.size}")
                    afterAppend.forEachIndexed { index, post ->
                        println("[DATABASE_ORDER] $index | ${post.id} | ${post.cachedAt}")
                    }
                }

                println("""
$DB_TRACE MEDIATOR END
$DB_TRACE loadType=$loadType
$DB_TRACE page=$page
$DB_TRACE time=${kotlin.time.Clock.System.now().toEpochMilliseconds()}
$DB_TRACE =========================
""")

                response.activeIssuesCount?.let { count ->
                    localDataSource.cacheActiveIssues(postLevel, count)
                }

                val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
                cacheMetadataDao.insertMetadata(
                    CacheMetadataEntity(
                        cacheKey = CacheMetadataEntity.postsKey(postLevel.name),
                        lastFetchedAt = now,
                    ),
                )


                MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
            }
            is DataState.Error -> {
                MediatorResult.Error(result.exception)
            }
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
