package org.example.project.core.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import org.example.project.core.model.home.Comment
import org.example.project.core.model.home.toComment
import org.example.project.core.network.NetworkMonitor
import org.example.project.core.network.services.PostService
import org.example.project.core.utils.DataState
import org.example.project.core.utils.safeApiCall

class CommentPagingSource(
    private val postService: PostService,
    private val postId: String,
    private val networkMonitor: NetworkMonitor,
) : PagingSource<Int, Comment>() {

    override fun getRefreshKey(state: PagingState<Int, Comment>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Comment> {
        val page = params.key ?: 0
        return when (val result = safeApiCall(networkMonitor) {
            postService.getComments(
                id = postId,
                page = page,
                limit = params.loadSize
            )
        }) {
            is DataState.Success -> {
                val response = result.data
            LoadResult.Page(
                data = response.items.map { it.toComment() },
                prevKey = response.prevKey,
                nextKey = response.nextKey
            )
            }
            is DataState.Error -> LoadResult.Error(result.exception)
            DataState.Loading -> LoadResult.Error(IllegalStateException("Unexpected paging loading state"))
        }
    }
}
