package org.example.project.core.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import org.example.project.core.model.home.Comment
import org.example.project.core.model.home.toComment
import org.example.project.core.network.services.PostService

class CommentPagingSource(
    private val postService: PostService,
    private val postId: String
) : PagingSource<Int, Comment>() {

    override fun getRefreshKey(state: PagingState<Int, Comment>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Comment> {
        val page = params.key ?: 0
        return try {
            val response = postService.getComments(
                id = postId,
                page = page,
                limit = params.loadSize
            )
            LoadResult.Page(
                data = response.items.map { it.toComment() },
                prevKey = response.prevKey,
                nextKey = response.nextKey
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
