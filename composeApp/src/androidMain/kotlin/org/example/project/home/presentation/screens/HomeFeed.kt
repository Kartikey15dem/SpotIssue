package org.example.project.home.presentation.screens

import androidx.compose.foundation.layout.Column
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
import org.example.project.core.components.PostCard
import org.example.project.core.model.home.Post
import org.example.project.feature.home.viewmodel.HomeIntent
import org.example.project.feature.home.viewmodel.HomeState
import org.example.project.theme.IssueSpotTheme

@Composable
fun HomeFeed(
    listState: LazyListState,
    feedState: org.example.project.core.presentation.FeedState,
    state: HomeState,
    onIntent: (HomeIntent) -> Unit,
    feedUiState: FeedUiState,
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit
) {
    println("[KMP_PAGING]\nHomePostsList BODY\nitemCount=${feedState.posts.size}\nrefreshing=${feedState.isRefreshing}\nloading=${feedState.isLoading}")
    
    val spacing = IssueSpotTheme.spacing

    Column(modifier = modifier) {
        header()
        Spacer(Modifier.height(spacing.small))
        
        println("[PAGING_TRACE] LazyColumn recomposed | itemCount=${feedState.posts.size} | time=${System.currentTimeMillis()}")
        
        // Threshold check to load more
        LaunchedEffect(listState) {
            snapshotFlow {
                listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
            }
            .distinctUntilChanged()
            .collect { lastVisibleItem ->
                if (lastVisibleItem != null) {
                    val totalItems = listState.layoutInfo.totalItemsCount
                    if (totalItems > 0 && lastVisibleItem >= totalItems - org.example.project.core.window.FeedConfig.LOAD_MORE_THRESHOLD) {
                        onIntent(HomeIntent.LoadMorePosts)
                    }
                }
            }
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
                    println("[KMP_PAGING]\nROW\npostId=${post.id}")
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
            HomeFeedFooter(
                state = feedUiState.footerState,
                onRetry = { onIntent(HomeIntent.RetryPosts) }
            )
        }
    }
}
}
