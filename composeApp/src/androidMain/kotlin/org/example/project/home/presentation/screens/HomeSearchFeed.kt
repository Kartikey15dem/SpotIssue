package org.example.project.home.presentation.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import org.example.project.core.components.InfiniteScrollHandler
import org.example.project.core.components.PostCard
import org.example.project.core.presentation.FeedState
import org.example.project.theme.IssueSpotTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import org.example.project.core.window.FeedConfig
import org.example.project.feature.home.viewmodel.HomeIntent

@Composable
fun HomeSearchFeed(
    searchState: FeedState,
    onIntent: (HomeIntent) -> Unit,
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit
) {
    val spacing = IssueSpotTheme.spacing
    val listState = rememberLazyListState()
    val itemCount = searchState.posts.size
    val showInitialLoading = itemCount == 0 && searchState.isLoading
    val showInitialError = itemCount == 0 && searchState.error != null
    val showEmptyFeed = itemCount == 0 && !searchState.isLoading && !searchState.isRefreshing && searchState.error == null
    val appendError = searchState.appendError
    val showNoMorePosts = itemCount > 0 && !searchState.hasMore && appendError == null && !searchState.isAppending

    InfiniteScrollHandler(listState = listState) {
        onIntent(HomeIntent.LoadMoreSearchPosts)
    }

    Column(modifier = modifier) {
        header()
        Spacer(Modifier.height(spacing.small))

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            item {
                if (showInitialLoading) {
                    HomeInitialLoading()
                }
                if (showInitialError) {
                    HomeInitialError(
                        message = searchState.error?.message ?: "An error occurred",
                        onRetry = { onIntent(HomeIntent.RetrySearchPosts) }
                    )
                }
                if (showEmptyFeed) {
                    HomeEmptyFeed()
                }
            }

            items(
                items = searchState.posts,
                key = { it.id },
                contentType = { null }
            ) { post ->
                    PostCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = spacing.smallMedium,
                                end = spacing.smallMedium,
                                bottom = spacing.smallMedium
                            ),
                        post = post,
                        isLiked = post.isLiked,
                        likesCount = post.likes,
                        commentsCount = post.comments,
                        isReported = post.isReported,
                        onLikeClick = {
                            onIntent(HomeIntent.LikeClicked(post.id))
                        },
                        onCommentIconClick = {
                            onIntent(HomeIntent.CommentsIconClicked(post.id))
                        },
                        onShareClick = { onIntent(HomeIntent.ShareClicked(post)) },
                        onReportClick = { reason ->
                            onIntent(HomeIntent.ReportClicked(post.id, reason))
                        },
                        onPostClick = { onIntent(HomeIntent.PostClicked(post.id)) }
                    )
            }

            item {
                val footerState = when {
                    searchState.isAppending -> FooterState.Loading
                    appendError != null -> FooterState.Error(Throwable(appendError.message))
                    showNoMorePosts -> FooterState.EndReached
                    else -> FooterState.Hidden
                }
                HomeFeedFooter(
                    state = footerState,
                    onRetry = { onIntent(HomeIntent.RetrySearchPosts) }
                )
            }
        }
    }
}
