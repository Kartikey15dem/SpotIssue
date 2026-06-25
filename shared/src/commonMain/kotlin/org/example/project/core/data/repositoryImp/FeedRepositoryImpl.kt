package org.example.project.core.data.repositoryImp

import co.touchlab.kermit.Logger
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.paging.ExperimentalPagingApi
import org.example.project.core.data.paging.FeedPostsRemoteMediator
import org.example.project.core.database.IssueSpotDatabase
import org.example.project.core.network.services.HomeService
import org.example.project.core.data.local.FeedLocalDataSource
import org.example.project.core.data.repository.FeedRepository
import org.example.project.core.database.entities.toPost
import org.example.project.core.model.auth.UserLocation
import org.example.project.core.model.home.Post
import org.example.project.core.model.home.PostLevel
import org.example.project.core.utils.NetworkMonitor

import org.example.project.core.data.paging.SearchPostsPagingSource
import org.example.project.core.utils.DataState
import org.example.project.core.utils.safeApiCall
import org.example.project.core.data.mappers.toPost

class FeedRepositoryImpl(
    private val homeService: HomeService,
    private val database: IssueSpotDatabase,
    private val localDataSource: FeedLocalDataSource,
    private val networkMonitor: NetworkMonitor,
) : FeedRepository {
    private val logger = Logger.withTag("FeedRepository")

    @OptIn(ExperimentalPagingApi::class)
    override fun getPagedPosts(postLevel: PostLevel, userLocation: UserLocation, forceRefresh: Boolean): Flow<PagingData<Post>> {
        return Pager(
            config = PagingConfig(pageSize = 5),
            remoteMediator = FeedPostsRemoteMediator(
                postLevel = postLevel,
                userLocation = userLocation,
                homeService = homeService,
                database = database,
                localDataSource = localDataSource,
                forceRefresh = forceRefresh,
                networkMonitor = networkMonitor,
            ),
            pagingSourceFactory = { database.postDao().pagingSourceByLevel(postLevel.name) },
        ).flow.map { pagingData ->
            pagingData.map { entity -> entity.toPost() }
        }
    }

    override fun observeActiveIssuesCount(postLevel: PostLevel): Flow<Int> {
        return localDataSource.observeCachedActiveIssues(postLevel)
            .map { it ?: 0 }
    }

    override fun getPagedSearchPosts(query: String, postLevel: PostLevel): Flow<PagingData<Post>> {
        return Pager(
            config = PagingConfig(pageSize = 10),
            pagingSourceFactory = { SearchPostsPagingSource(homeService, query, postLevel, networkMonitor) }
        ).flow
    }

    override fun observePosts(postLevel: PostLevel): Flow<List<Post>> {
        return database.postDao().observePostsByLevel(postLevel.name)
            .map { entities -> entities.map { it.toPost() } }
    }

    override suspend fun refreshPosts(
        postLevel: PostLevel,
        userLocation: UserLocation
    ): DataState<List<Post>> = safeApiCall(networkMonitor) {
        val posts = homeService.getPosts(
            level = postLevel.name,
            locality = userLocation.locality,
            district = userLocation.district,
            state = userLocation.state,
            country = userLocation.country,
            lat = userLocation.latitude,
            lon = userLocation.longitude,
            page = 1,
            limit = 10
        ).items.map { it.toPost() }
        localDataSource.cachePosts(postLevel, posts)
        posts
    }

    override suspend fun searchPosts(query: String, postLevel: PostLevel): DataState<List<Post>> =
        safeApiCall(networkMonitor) {
            homeService.searchPosts(query, postLevel.name, page = 1, limit = 100)
                .items
                .map { it.toPost() }
        }

    override suspend fun updateLikeStatus(postId: String, likesCount: Int, isLiked: Boolean) {
        database.postDao().updateLikeStatus(postId, likesCount, isLiked)
    }

    override suspend fun updateReportStatus(postId: String, isReported: Boolean) {
        database.postDao().updateReportStatus(postId, isReported)
    }
}
