package org.example.project.core.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import org.example.project.core.model.home.Post
import org.example.project.core.model.home.PostLevel
import org.example.project.core.utils.NetworkMonitor
import org.example.project.core.network.services.HomeService
import org.example.project.core.data.mappers.toPost
import org.example.project.core.utils.DataState
import org.example.project.core.utils.safeApiCall

class SearchPostsPagingSource(
    private val homeService: HomeService,
    private val query: String,
    private val postLevel: PostLevel,
    private val networkMonitor: NetworkMonitor,
) : PagingSource<Int, Post>() {

    override fun getRefreshKey(state: PagingState<Int, Post>): Int? {
        return state.anchorPosition?.let { position ->
            state.closestPageToPosition(position)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(position)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Post> {
        val page = params.key ?: 0
        val limit = params.loadSize

        return when (val result = safeApiCall(networkMonitor) {
            homeService.searchPosts(
                query = query,
                level = postLevel.name,
                page = page,
                limit = limit,
            )
        }) {
            is DataState.Success -> {
                val response = result.data
            val posts = response.items.map { it.toPost() }
            val endReached = response.nextKey == null || posts.isEmpty()

            LoadResult.Page(
                data = posts,
                prevKey = response.prevKey,
                nextKey = if (endReached) null else response.nextKey,
            )
            }
            is DataState.Error -> LoadResult.Error(result.exception)
            DataState.Loading -> LoadResult.Error(IllegalStateException("Unexpected paging loading state"))
        }
    }
}
