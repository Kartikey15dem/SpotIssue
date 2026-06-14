package org.example.project.home.presentation.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.paging.LoadState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import kotlinx.coroutines.flow.collectLatest
import org.example.project.R
import org.example.project.core.model.home.getText
import org.example.project.core.components.PostCard
import org.example.project.core.components.PostLevelChip
import org.example.project.feature.home.viewmodel.HomeIntent
import org.example.project.feature.home.viewmodel.HomeSideEffect
import org.example.project.feature.home.viewmodel.HomeState
import org.example.project.feature.home.viewmodel.HomeViewModel
import org.example.project.theme.IssueSpotColors
import org.example.project.theme.IssueSpotTypography
import org.example.project.core.components.CommentsBottomSheet
import org.example.project.theme.IssueSpotTheme
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.material3.Snackbar

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onNavigateToCreatePost: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    viewModel: HomeViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.sideEffects.collectLatest { effect ->
            when (effect) {
                is HomeSideEffect.NavigateToCreatePost -> onNavigateToCreatePost()
                is HomeSideEffect.NavigateToProfile -> onNavigateToProfile()
                is HomeSideEffect.ShowError -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
                is HomeSideEffect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
                is HomeSideEffect.SharePost -> {
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, effect.text)
                        type = "text/plain"
                    }
                    val shareIntent = Intent.createChooser(sendIntent, "Share Issue via...")
                    context.startActivity(shareIntent)
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { 
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = Color(0xFF323232),
                    contentColor = Color.White,
                    actionColor = Color(0xFF4A6CF7)
                )
            }
        },
        containerColor = IssueSpotColors.Background
    ) { padding ->
        HomeContent(
            modifier = modifier.padding(padding),
            state = state,
            onIntent = viewModel::onIntent,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(
    modifier: Modifier = Modifier,
    state: HomeState,
    onIntent: (HomeIntent) -> Unit,
) {
    val activeFlow = if (state.query.isNotBlank() && state.searchPostsFlow != null) state.searchPostsFlow else state.postsFlow
    val pagingItems = activeFlow?.collectAsLazyPagingItems()
    var isManualRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(pagingItems?.loadState?.refresh) {
        if (pagingItems?.loadState?.refresh !is LoadState.Loading) {
            isManualRefreshing = false
        }
    }
    
    val isRefreshing = isManualRefreshing



    val spacing = IssueSpotTheme.spacing
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { onIntent(HomeIntent.Refresh) },
        modifier = modifier
            .fillMaxSize()
            .background(IssueSpotColors.Background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            if(state.expandedPost != null) {
                val post = state.expandedPost
                val override = state.postOverrides[post.id]

                val isLiked = override?.isLiked ?: post.isLiked
                val resolvedLikes = override?.likesCount ?: post.likes
                val resolvedComments = override?.commentsCount ?: post.comments
                val isReported = override?.isReported ?: post.isReported

                BackHandler {
                    onIntent(HomeIntent.DismissPost)
                }


                    PostCard(
                        modifier = Modifier
                            .fillMaxSize(),
                        post = post,
                        isLiked = isLiked,
                        likesCount = resolvedLikes,
                        commentsCount = resolvedComments,
                        isReported = isReported,

                        onLikeClick = {
                            onIntent(
                                HomeIntent.LikeClicked(
                                    post.id,
                                    isLiked,
                                    resolvedLikes
                                )
                            )
                        },
                        onCommentIconClick = {
                            onIntent(
                                HomeIntent.CommentsIconClicked(
                                    post.id,
                                    resolvedComments
                                )
                            )
                        },
                        onShareClick = { onIntent(HomeIntent.ShareClicked(post)) },
                        onReportClick = { reason ->
                            onIntent(HomeIntent.ReportClicked(post.id, reason))
                        },
                        onCollapseClick = { onIntent(HomeIntent.DismissPost) },
                        isDetailMode = true

                    )
                    


            } else {
                    HomeHeader(
                        modifier = Modifier.fillMaxWidth(),
                        state = state,
                        onIntent = onIntent
                    )

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(spacing.smallMedium)
                    ) {
                        item { Spacer(Modifier.height(spacing.small)) }

                        if (pagingItems != null) {
                            items(
                                count = pagingItems.itemCount,
                                key = pagingItems.itemKey { it.id }
                            ) { index ->
                                val post = pagingItems[index]
                                if (post != null) {
                                    val override = state.postOverrides[post.id]

                                    val isLiked = override?.isLiked ?: post.isLiked
                                    val resolvedLikes = override?.likesCount ?: post.likes
                                    val resolvedComments = override?.commentsCount ?: post.comments
                                    val isReported = override?.isReported ?: post.isReported

                                    PostCard(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = spacing.smallMedium),
                                        post = post,
                                        isLiked = isLiked,
                                        likesCount = resolvedLikes,
                                        commentsCount = resolvedComments,
                                        isReported = isReported,

                                        onLikeClick = {
                                            onIntent(
                                                HomeIntent.LikeClicked(
                                                    post.id,
                                                    isLiked,
                                                    resolvedLikes
                                                )
                                            )
                                        },
                                        onCommentIconClick = {
                                            onIntent(
                                                HomeIntent.CommentsIconClicked(
                                                    post.id,
                                                    resolvedComments
                                                )
                                            )
                                        },
                                        onShareClick = { onIntent(HomeIntent.ShareClicked(post)) },
                                        onReportClick = { reason ->
                                            onIntent(HomeIntent.ReportClicked(post.id, reason))
                                        },
                                        onPostClick = { onIntent(HomeIntent.PostClicked(post)) }
                                    )
                                }
                            }
                        } else {
                            item {
                                Box(
                                    modifier = Modifier.fillParentMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = IssueSpotColors.Primary)
                                }
                            }
                        }

                        item { Spacer(Modifier.height(spacing.medium)) }

                        val refreshError = pagingItems?.loadState?.refresh as? LoadState.Error
                        val appendError = pagingItems?.loadState?.append as? LoadState.Error

                        if (refreshError != null && pagingItems.itemCount == 0) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(spacing.medium),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = refreshError.error.message ?: "An error occurred",
                                        color = IssueSpotColors.OnBackground,
                                        style = IssueSpotTypography.bodyMedium
                                    )
                                }
                            }
                        } else if (pagingItems?.loadState?.refresh is LoadState.Loading && pagingItems.itemCount == 0) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(spacing.medium),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = IssueSpotColors.Primary)
                                }
                            }
                        }

                        if (appendError != null && pagingItems.itemCount > 0) {
                            item {
                                Text(
                                    modifier = Modifier.fillMaxWidth().padding(spacing.medium),
                                    text = appendError.error.message ?: "An error occurred",
                                    color = IssueSpotColors.OnBackground,
                                    style = IssueSpotTypography.bodyMedium
                                )
                            }
                        }
                    }
                }


        }
    }

    if (state.showCommentsSheetForPostId != null) {
        val activePostId = state.showCommentsSheetForPostId
        val activeOverride = state.postOverrides[activePostId]
        val commentsPagingItems = activeOverride?.commentsFlow?.collectAsLazyPagingItems()

        CommentsBottomSheet(
            comments = commentsPagingItems,
            onDismiss = { onIntent(HomeIntent.DismissCommentsSheet) },
            onSubmit = { text ->
                val fallbackCount = pagingItems?.itemSnapshotList?.items?.find { it.id == activePostId }?.comments ?: 0
                onIntent(
                    HomeIntent.CommentSubmitted(
                        postId = activePostId,
                        commentText = text,
                        currentCommentCount = activeOverride?.commentsCount ?: fallbackCount
                    )
                )
            },
            currentUserImageUrl = state.currentUserImage
        )
    }
}

@Composable
private fun HomeHeader(
    modifier: Modifier = Modifier,
    state: HomeState,
    onIntent: (HomeIntent) -> Unit,
) {
    val spacing = IssueSpotTheme.spacing
    val shapes = MaterialTheme.shapes
    Column(
        modifier = modifier
            .background(IssueSpotColors.Surface)
            .padding(horizontal = spacing.smallMedium, vertical = spacing.small)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = { onIntent(HomeIntent.SearchQueryChanged(it)) },
                placeholder = {
                    Text(
                        text = "Search issues, users, location",
                        style = IssueSpotTypography.bodyLarge,
                        color = IssueSpotColors.OnSurfaceVariant
                    )
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_search),
                        contentDescription = "Search",
                        tint = IssueSpotColors.OnSurfaceVariant
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = IssueSpotColors.SurfaceVariant,
                    focusedContainerColor = IssueSpotColors.SurfaceVariant,
                    unfocusedBorderColor = IssueSpotColors.SurfaceVariant,
                    focusedBorderColor = IssueSpotColors.SurfaceVariant,
                    unfocusedTextColor = IssueSpotColors.OnSurface,
                    focusedTextColor = IssueSpotColors.OnSurface,
                )
            )

            Spacer(Modifier.width(spacing.small))

            Button(
                onClick = { onIntent(HomeIntent.CreatePostClicked) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = IssueSpotColors.PostButtonBackground,
                    contentColor = IssueSpotColors.PostButtonText
                ),
                shape = shapes.medium,
                modifier = Modifier.defaultMinSize(minWidth = 0.dp),
                contentPadding = PaddingValues(horizontal = spacing.smallMedium, vertical = spacing.extraSmall),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(spacing.small))
                Text("Post", style = IssueSpotTypography.bodyLarge)
            }

            Spacer(Modifier.width(spacing.small))

            IconButton(onClick = { onIntent(HomeIntent.ProfileClicked) }) {
                Icon(
                    painter = painterResource(R.drawable.ic_person),
                    contentDescription = "Profile",
                    tint = IssueSpotColors.OnSurface
                )
            }
        }

        if (state.query.isBlank()) {
            Spacer(Modifier.height(spacing.smallMedium))

            Row(verticalAlignment = Alignment.CenterVertically) {

                PostLevelChip(postLevel = state.postLevel)

                Spacer(Modifier.width(spacing.small))



                Text(
                    text = "${state.activeIssues} active issues",
                    style = IssueSpotTypography.bodyLarge,
                    color = IssueSpotColors.OnSurfaceVariant
                )
            }

            Spacer(Modifier.height(spacing.smallMedium))

            Text(
                text = "${state.postLevel.displayName} Issues",
                style = IssueSpotTypography.bodyLarge,
                color = IssueSpotColors.OnSurface,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(spacing.extraSmall))

            Text(
                text = state.postLevel.getText(),
                style = IssueSpotTypography.bodyLarge,
                color = IssueSpotColors.OnSurfaceVariant
            )
        }
    }
}
