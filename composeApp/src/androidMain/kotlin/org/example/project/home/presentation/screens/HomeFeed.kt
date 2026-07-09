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
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import org.example.project.core.components.PostCard
import org.example.project.core.model.home.Post
import org.example.project.feature.home.viewmodel.HomeIntent
import org.example.project.feature.home.viewmodel.HomeState
import org.example.project.theme.IssueSpotTheme

@Composable
fun HomeFeed(
    listState: LazyListState,
    pagingItems: LazyPagingItems<Post>,
    pagingState: PagingPresentationState,
    state: HomeState,
    onIntent: (HomeIntent) -> Unit,
    feedUiState: FeedUiState,
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit
) {
    println("[KMP_PAGING]\nHomePostsList BODY\nitemCount=${pagingItems.itemCount}\nrefreshing=${pagingState.isRefreshing}\nappending=${pagingState.isAppending}")
    
    val spacing = IssueSpotTheme.spacing

    Column(modifier = modifier) {
        header()
        Spacer(Modifier.height(spacing.small))
        
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
                    onRetry = { pagingItems.refresh() }
                )
            }
            if (feedUiState.showEmptyFeed) {
                HomeEmptyFeed()
            }
        }

        if (feedUiState.showFeed) {
            items(
                count = pagingItems.itemCount,
                key = pagingItems.itemKey { it.id },
                contentType = pagingItems.itemContentType { null }
            ) { index ->
                val post = pagingItems[index]
                if (post != null) {
                    val isFirstOrLast = index == 0 || index >= pagingItems.itemCount - 5
                    if (isFirstOrLast) {
                        println("[KMP_PAGING]\nROW\nindex=$index\npostId=${post.id}")
                    }
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
            }
        }

        item {
            HomeFeedFooter(
                state = feedUiState.footerState,
                onRetry = { pagingItems.retry() }
            )
        }
    }
}
}
