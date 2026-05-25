package org.example.project.core.data.repositoryImp

import co.touchlab.kermit.Logger
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import androidx.paging.ExperimentalPagingApi
import org.example.project.core.data.paging.FeedPostsRemoteMediator
import org.example.project.core.database.IssueSpotDatabase
import org.example.project.core.network.services.HomeService
import org.example.project.core.data.mappers.toPost
import org.example.project.core.data.local.FeedLocalDataSource
import org.example.project.core.data.repository.FeedRepository
import org.example.project.core.database.entities.toPost
import org.example.project.core.model.home.Post
import org.example.project.core.model.home.PostLevel
import org.example.project.core.utils.DataState

class FeedRepositoryImpl(
    private val homeService: HomeService,
    private val database: IssueSpotDatabase,
    private val localDataSource: FeedLocalDataSource,
) : FeedRepository {
    private val logger = Logger.Companion.withTag("FeedRepository")

    @OptIn(ExperimentalPagingApi::class)
    override fun getPagedPosts(postLevel: PostLevel, forceRefresh: Boolean): Flow<PagingData<Post>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            remoteMediator = FeedPostsRemoteMediator(
                postLevel = postLevel,
                homeService = homeService,
                database = database,
                localDataSource = localDataSource,
                forceRefresh = forceRefresh,
            ),
            pagingSourceFactory = { database.postDao().pagingSourceByLevel(postLevel.name) },
        ).flow.map { pagingData ->
            pagingData.map { entity -> entity.toPost() }
        }
    }

    override fun observeActiveIssuesCount(postLevel: PostLevel): Flow<DataState<Int>> = flow {

        val dbFlow = localDataSource.observeCachedActiveIssues(postLevel)
            .map { DataState.Success(it ?: 0) as DataState<Int> }

        emitAll(dbFlow)
    }.onStart { emit(DataState.Loading) }
}
