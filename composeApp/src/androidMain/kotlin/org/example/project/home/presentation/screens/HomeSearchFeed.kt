package org.example.project.home.presentation.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import androidx.paging.compose.itemContentType
import org.example.project.core.components.PostCard
import org.example.project.core.model.home.Post
import org.example.project.feature.home.viewmodel.HomeIntent
import org.example.project.theme.IssueSpotTheme

@Composable
fun HomeSearchFeed(
    searchPagingItems: LazyPagingItems<Post>,
    onIntent: (HomeIntent) -> Unit,
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit
) {
    val spacing = IssueSpotTheme.spacing

    Column(modifier = modifier) {
        header()
        Spacer(Modifier.height(spacing.small))

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            item {
                if (searchPagingItems.loadState.refresh is LoadState.Loading) {
                    HomeInitialLoading()
                }
            }

            items(
                count = searchPagingItems.itemCount,
                key = searchPagingItems.itemKey { it.id },
                contentType = searchPagingItems.itemContentType { null }
            ) { index ->
                val post = searchPagingItems[index]
                if (post != null) {
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

            item {
                when (searchPagingItems.loadState.append) {
                    is LoadState.Loading -> {
                        HomeFeedFooter(state = FooterState.Loading, onRetry = {})
                    }
                    is LoadState.Error -> {
                        HomeFeedFooter(state = FooterState.Error(Throwable("Error loading more")), onRetry = { searchPagingItems.retry() })
                    }
                    else -> {}
                }
            }
        }
    }
}
