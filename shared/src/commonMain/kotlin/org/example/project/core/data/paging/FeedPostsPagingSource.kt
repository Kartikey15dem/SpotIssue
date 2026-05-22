package org.example.project.core.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import org.example.project.core.data.local.FeedLocalDataSource
import org.example.project.core.model.home.Post
import org.example.project.core.model.home.PostLevel
import org.example.project.core.network.services.HomeService
import org.example.project.core.data.mappers.toPost

/**
 * PagingSource that fetches feed posts directly from the network.
 *
 * This intentionally mirrors the android-client "ClientListPagingSource" style:
 * - No RemoteMediator / DB-backed paging source.
 * - Refresh behavior is controlled by recreating the Pager flow in the ViewModel.
 */
class FeedPostsPagingSource(
    private val homeService: HomeService,
    private val postLevel: PostLevel,
    private val localDataSource: FeedLocalDataSource,
) : PagingSource<Int, Post>() {

    override fun getRefreshKey(state: PagingState<Int, Post>): Int? {
        return state.anchorPosition?.let { position ->
            state.closestPageToPosition(position)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(position)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Post> {
        val page = params.key ?: 1
        val limit = params.loadSize

        return try {
            val response = homeService.getPosts(
                level = postLevel.name,
                page = page,
                limit = limit,
            )

            val posts = response.items.map { it.toPost() }
            val endReached = posts.isEmpty()

            // Cache network result into Room so offline paging has data.
            if (page == 1) {
                localDataSource.cachePosts(postLevel, posts)
            } else if (posts.isNotEmpty()) {
                localDataSource.appendPosts(postLevel, posts)
            }

            LoadResult.Page(
                data = posts,
                prevKey = if (page == 1) null else page - 1,
                nextKey = if (endReached) null else page + 1,
            )
        } catch (t: Throwable) {
            LoadResult.Error(t)
        }
    }
}