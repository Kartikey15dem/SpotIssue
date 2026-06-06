package org.example.project.core.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import org.example.project.core.model.home.Post
import org.example.project.core.model.home.PostLevel
import org.example.project.core.network.services.HomeService
import org.example.project.core.data.mappers.toPost

class SearchPostsPagingSource(
    private val homeService: HomeService,
    private val query: String,
    private val postLevel: PostLevel,
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
            val response = homeService.searchPosts(
                query = query,
                level = postLevel.name,
                page = page,
                limit = limit,
            )

            val posts = response.items.map { it.toPost() }
            val endReached = posts.isEmpty()

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