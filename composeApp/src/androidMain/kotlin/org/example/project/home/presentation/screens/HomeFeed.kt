
package org.example.project.home.presentation.screens

import androidx.compose.foundation.layout.Column
import org.example.project.core.window.FeedConfig
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import org.example.project.core.components.InfiniteScrollHandler
import org.example.project.core.components.PostCard
import org.example.project.core.model.home.Post
import org.example.project.core.presentation.FeedState
import org.example.project.feature.home.viewmodel.HomeIntent
import org.example.project.feature.home.viewmodel.HomeState
import org.example.project.theme.IssueSpotTheme

@Composable
fun HomeFeed(
    listState: LazyListState,
    feedState: FeedState,
    onIntent: (HomeIntent) -> Unit,
    feedUiState: FeedUiState,
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit
) {
    
    val spacing = IssueSpotTheme.spacing

    Column(modifier = modifier) {
        header()
        Spacer(Modifier.height(spacing.small))
        
        // PAGING PIPELINE STEP 5: UI SCROLL TRIGGER
        // We use InfiniteScrollHandler to monitor how far down the user has scrolled.
        // When they get near the bottom, it fires an intent to load the next page.
        InfiniteScrollHandler(listState = listState) {
            onIntent(HomeIntent.LoadMorePosts)
        }
        
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            item {
            if (feedUiState.showInitialLoading) {
                HomeInitialLoading()
            }
            if (feedUiState.showInitialError) {
                HomeInitialError(
                    message = feedUiState.refreshError?.message ?: "An error occurred",
                    onRetry = { onIntent(HomeIntent.RefreshPosts()) }
                )
            }
            if (feedUiState.showEmptyFeed) {
                HomeEmptyFeed()
            }
        }

        if (feedUiState.showFeed) {
            items(
                items = feedState.posts,
                key = { it.id },
                contentType = { null }
            ) { post ->
                val isFirstOrLast = post == feedState.posts.firstOrNull() || post == feedState.posts.lastOrNull()
                if (isFirstOrLast) {
                }
                    PostCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem()
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
        }

        item {
            // PAGING PIPELINE STEP 6: UI PAGINATION INDICATOR
            // Renders a loading spinner or an error/retry button at the very bottom of the feed.
            // Depends on `feedUiState.footerState` (which represents if we are appending items or failed to append).
            HomeFeedFooter(
                state = feedUiState.footerState,
                onRetry = { onIntent(HomeIntent.RetryPosts) }
            )
        }
    }
}
}
