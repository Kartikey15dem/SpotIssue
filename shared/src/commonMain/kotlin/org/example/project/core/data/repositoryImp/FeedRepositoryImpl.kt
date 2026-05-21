package org.example.project.core.data.repositoryImp

import co.touchlab.kermit.Logger
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import org.example.project.core.data.paging.PostRemoteMediator
import org.example.project.core.database.IssueSpotDatabase
import org.example.project.core.database.entities.toPost
import org.example.project.core.network.services.HomeService
import org.example.project.home.data.local.FeedLocalDataSource
import org.example.project.home.domain.models.Post
import org.example.project.home.domain.models.PostLevel
import org.example.project.core.data.repository.FeedRepository
import org.example.project.core.utils.DataState
import org.example.project.core.utils.safeApiCall
import org.example.project.core.utils.asDataStateFlow

class FeedRepositoryImpl(
    private val homeService: HomeService,
    private val database: IssueSpotDatabase,
    private val localDataSource: FeedLocalDataSource
) : FeedRepository {
    private val logger = Logger.Companion.withTag("FeedRepository")

    @OptIn(ExperimentalPagingApi::class)
    override fun getPagedPosts(postLevel: PostLevel): Flow<PagingData<Post>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            remoteMediator = PostRemoteMediator(homeService, database, postLevel),
            pagingSourceFactory = { database.postDao().pagingSourceByLevel(postLevel.name) }
        ).flow.map { pagingData ->
            pagingData.map { it.toPost() }
        }
    }

    override fun observeActiveIssuesCount(postLevel: PostLevel): Flow<DataState<Int>> = flow {
        // First emit from DB
        val dbFlow = localDataSource.observeCachedActiveIssues(postLevel)
            .map { DataState.Success(it ?: 0) as DataState<Int> }
        
        // Trigger refresh in background if stale
        if (localDataSource.isActiveIssuesCacheStale(postLevel)) {
            refreshActiveIssuesCount(postLevel)
        }
        
        emitAll(dbFlow)
    }.onStart { emit(DataState.Loading) }

    override suspend fun refreshPosts(postLevel: PostLevel): DataState<Unit> = safeApiCall {
        val response = homeService.getPosts(
            level = postLevel.name,
            page = 1,
            limit = 50
        )
        val posts = response.items.map { it.toPost() }
        localDataSource.cachePosts(postLevel, posts)
        Unit
    }

    override suspend fun refreshActiveIssuesCount(postLevel: PostLevel): DataState<Unit> = safeApiCall {
        val result = homeService.getActiveIssuesCount(postLevel.name)
        localDataSource.cacheActiveIssues(postLevel, result.totalActiveIssues)
        Unit
    }
}
