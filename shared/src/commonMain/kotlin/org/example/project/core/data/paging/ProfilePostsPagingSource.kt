package org.example.project.core.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import org.example.project.core.data.local.ProfileLocalDataSource
import org.example.project.core.network.services.ProfileService
import org.example.project.core.data.mappers.toPost
import org.example.project.home.domain.models.Post

class ProfilePostsPagingSource(
    private val profileService: ProfileService,
    private val userId: String,
    private val kind: Kind,
    private val localDataSource: ProfileLocalDataSource,
) : PagingSource<Int, Post>() {

    enum class Kind { USER_POSTS, LIKED_POSTS }

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
            val response = when (kind) {
                Kind.USER_POSTS -> profileService.getUserPosts(userId = userId, page = page, limit = limit)
                Kind.LIKED_POSTS -> profileService.getLikedPosts(userId = userId, page = page, limit = limit)
            }

            val posts = response.items.map { it.toPost() }
            val endReached = posts.isEmpty()

            // Cache into Room for offline paging.
            if (page == 1) {
                when (kind) {
                    Kind.USER_POSTS -> localDataSource.cacheUserPosts(posts)
                    Kind.LIKED_POSTS -> localDataSource.cacheLikedPosts(posts)
                }
            } else if (posts.isNotEmpty()) {
                when (kind) {
                    Kind.USER_POSTS -> localDataSource.appendUserPosts(posts)
                    Kind.LIKED_POSTS -> localDataSource.appendLikedPosts(posts)
                }
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