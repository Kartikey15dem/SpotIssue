package org.example.project.home.presentation.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.collectLatest
import org.example.project.R
import org.example.project.core.components.CommentsBottomSheet
import org.example.project.core.components.PostCard
import org.example.project.core.components.PostLevelChip
import org.example.project.core.model.home.Comment
import org.example.project.core.model.home.Post
import org.example.project.core.model.home.getText
import org.example.project.feature.home.viewmodel.HomeIntent
import org.example.project.feature.home.viewmodel.HomeSideEffect
import org.example.project.feature.home.viewmodel.HomeState
import org.example.project.feature.home.viewmodel.HomeViewModel
import org.example.project.theme.IssueSpotColors
import org.example.project.theme.IssueSpotTheme
import org.example.project.theme.IssueSpotTypography
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onNavigateToCreatePost: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    viewModel: HomeViewModel = koinViewModel()
) {
    println("[KMP_PAGING]\nHomeScreen BODY")
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentLevel by viewModel.currentLevel.collectAsStateWithLifecycle()
    val activeIssues by viewModel.activeIssues.collectAsStateWithLifecycle()
    val expandedPost by viewModel.expandedPost.collectAsStateWithLifecycle()
    val pagingItems = viewModel.pagedPosts.collectAsLazyPagingItems()
    println("[PAGING_UI] LAZY PAGING ITEMS | identity=${System.identityHashCode(pagingItems)} | itemCount=${pagingItems.itemCount} | refresh=${pagingItems.loadState.refresh} | append=${pagingItems.loadState.append} | mediatorRefresh=${pagingItems.loadState.mediator?.refresh} | mediatorAppend=${pagingItems.loadState.mediator?.append} | time=${kotlin.time.Clock.System.now()}")
    
    val activeCommentsFlow by viewModel.activeCommentsFlow.collectAsStateWithLifecycle()
    val activePostId = state.showCommentsSheetForPostId
    
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(expandedPost) {
        println("[KMP_PAGING]\nExpandedPost Changed\npostId: ${expandedPost?.id ?: "nil"}")
    }
    LaunchedEffect(currentLevel) {
        println("[KMP_PAGING]\nCurrentLevel Changed\nlevel: $currentLevel")
    }
    LaunchedEffect(activeCommentsFlow) {
        println("[KMP_PAGING]\nComments Flow Changed\npostId: $activePostId")
    }

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
            currentLevel = currentLevel,
            activeIssues = activeIssues,
            expandedPost = expandedPost,
            pagingItems = pagingItems,
            onIntent = viewModel::onIntent,
        )
    }

    val currentCommentsFlow = activeCommentsFlow
    if (activePostId != null && currentCommentsFlow != null) {
        val commentsPagingItems = currentCommentsFlow.collectAsLazyPagingItems()
        
        CommentsBottomSheet(
            comments = commentsPagingItems,
            onDismiss = { viewModel.onIntent(HomeIntent.DismissCommentsSheet) },
            onSubmit = { text ->
                viewModel.onIntent(
                    HomeIntent.CommentSubmitted(
                        postId = activePostId,
                        commentText = text
                    )
                )
            },
            currentUserImageUrl = state.currentUserImage
        )
    }
}

@Stable
data class PagingPresentationState(
    val isInitialLoading: Boolean,
    val isRefreshing: Boolean,
    val isAppending: Boolean,
    val refreshError: Throwable?,
    val appendError: Throwable?,
    val endReached: Boolean
)

@Composable
fun rememberPagingPresentationState(
    pagingItems: LazyPagingItems<Post>
): PagingPresentationState {
    val refreshState = pagingItems.loadState.refresh
    val mediatorRefreshState = pagingItems.loadState.mediator?.refresh

    val appendState = pagingItems.loadState.append
    val mediatorAppendState = pagingItems.loadState.mediator?.append

    val isRefreshing = refreshState is LoadState.Loading || mediatorRefreshState is LoadState.Loading
    val isAppending = appendState is LoadState.Loading || mediatorAppendState is LoadState.Loading
    
    val refreshError = (refreshState as? LoadState.Error)?.error ?: (mediatorRefreshState as? LoadState.Error)?.error
    val appendError = (appendState as? LoadState.Error)?.error ?: (mediatorAppendState as? LoadState.Error)?.error

    val endReached = (appendState is LoadState.NotLoading && appendState.endOfPaginationReached) ||
            (mediatorAppendState is LoadState.NotLoading && mediatorAppendState.endOfPaginationReached)

    val isInitialLoading = pagingItems.itemCount == 0 && isRefreshing

    println("[PAGING_UI] LOAD STATE UPDATE | identity=${System.identityHashCode(pagingItems)} | itemCount=${pagingItems.itemCount} | refresh=${pagingItems.loadState.refresh} | append=${pagingItems.loadState.append} | prepend=${pagingItems.loadState.prepend} | mediatorRefresh=${pagingItems.loadState.mediator?.refresh} | mediatorAppend=${pagingItems.loadState.mediator?.append} | isRefreshing=$isRefreshing | isAppending=$isAppending | time=${kotlin.time.Clock.System.now()}")

    return PagingPresentationState(
        isInitialLoading = isInitialLoading,
        isRefreshing = isRefreshing,
        isAppending = isAppending,
        refreshError = refreshError,
        appendError = appendError,
        endReached = endReached
    )
}

@Stable
data class FeedUiState(
    val showInitialLoading: Boolean,
    val showFeed: Boolean,
    val showEmptyFeed: Boolean,
    val showInitialError: Boolean,
    val footerState: FooterState,
    val refreshError: Throwable?,
    val isPullRefreshing: Boolean
)

@Composable
fun rememberFeedUiState(
    pagingState: PagingPresentationState,
    itemCount: Int
): FeedUiState {
    val showInitialLoading = pagingState.isInitialLoading
    val showInitialError = itemCount == 0 && pagingState.refreshError != null && !pagingState.isRefreshing
    val showEmptyFeed = itemCount == 0 && !pagingState.isRefreshing && pagingState.refreshError == null
    val showFeed = itemCount > 0
    
    val isPullRefreshing = pagingState.isRefreshing && itemCount > 0

    val shouldShowFooter = itemCount > 0 && !showInitialLoading && !pagingState.isRefreshing
    val showNoMorePosts = shouldShowFooter && pagingState.endReached && pagingState.appendError == null && !pagingState.isAppending

    val footerState = when {
        !shouldShowFooter -> FooterState.Hidden
        pagingState.isAppending -> FooterState.Loading
        pagingState.appendError != null -> FooterState.Error(pagingState.appendError)
        showNoMorePosts -> FooterState.EndReached
        else -> FooterState.Hidden
    }

    return FeedUiState(
        showInitialLoading = showInitialLoading,
        showFeed = showFeed,
        showEmptyFeed = showEmptyFeed,
        showInitialError = showInitialError,
        footerState = footerState,
        refreshError = pagingState.refreshError,
        isPullRefreshing = isPullRefreshing
    )
}

@Composable
fun HomeContent(
    modifier: Modifier = Modifier,
    state: HomeState,
    currentLevel: org.example.project.core.model.home.PostLevel,
    activeIssues: Int,
    expandedPost: Post?,
    pagingItems: LazyPagingItems<Post>,
    onIntent: (HomeIntent) -> Unit,
) {
    val localityState = rememberLazyListState()
    val districtState = rememberLazyListState()
    val stateState = rememberLazyListState()
    val nationalState = rememberLazyListState()

    val listState = when (currentLevel) {
        org.example.project.core.model.home.PostLevel.LOCALITY -> localityState
        org.example.project.core.model.home.PostLevel.DISTRICT -> districtState
        org.example.project.core.model.home.PostLevel.STATE -> stateState
        org.example.project.core.model.home.PostLevel.NATIONAL -> nationalState
    }
    
    val pagingState = rememberPagingPresentationState(pagingItems)
    LaunchedEffect(
        pagingItems.itemCount,
        pagingState.isRefreshing,
        pagingState.isAppending
    ) {
        println("[PAGING_UI] HOME CONTENT STATE | identity=${System.identityHashCode(pagingItems)} | itemCount=${pagingItems.itemCount} | refreshing=${pagingState.isRefreshing} | appending=${pagingState.isAppending} | time=${kotlin.time.Clock.System.now()}")
    }
    val feedUiState = rememberFeedUiState(pagingState, pagingItems.itemCount)

    LaunchedEffect(feedUiState.refreshError) {
        if (feedUiState.refreshError != null && pagingItems.itemCount > 0) {
            onIntent(HomeIntent.ShowRefreshErrorSnackbar(feedUiState.refreshError.message ?: "An error occurred"))
        }
    }

    LaunchedEffect(feedUiState.isPullRefreshing) {
        println("[KMP_PAGING]\nPull Refresh\n${feedUiState.isPullRefreshing}")
    }

    HomePullRefresh(
        isRefreshing = feedUiState.isPullRefreshing,
        onRefresh = { pagingItems.refresh() },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            if (expandedPost != null) {
                BackHandler {
                    onIntent(HomeIntent.DismissPost)
                }

                PostCard(
                    modifier = Modifier.fillMaxSize(),
                    post = expandedPost,
                    isLiked = expandedPost.isLiked,
                    likesCount = expandedPost.likes,
                    commentsCount = expandedPost.comments,
                    isReported = expandedPost.isReported,
                    onLikeClick = {
                        onIntent(HomeIntent.LikeClicked(expandedPost.id))
                    },
                    onCommentIconClick = {
                        onIntent(HomeIntent.CommentsIconClicked(expandedPost.id))
                    },
                    onShareClick = { onIntent(HomeIntent.ShareClicked(expandedPost)) },
                    onReportClick = { reason ->
                        onIntent(HomeIntent.ReportClicked(expandedPost.id, reason))
                    },
                    onCollapseClick = { onIntent(HomeIntent.DismissPost) },
                    isDetailMode = true
                )

            } else {
                HomeFeed(
                    listState = listState,
                    pagingItems = pagingItems,
                    pagingState = pagingState,
                    state = state,
                    onIntent = onIntent,
                    feedUiState = feedUiState,
                    modifier = Modifier.fillMaxSize(),
                    header = {
                        HomeHeader(
                            modifier = Modifier.fillMaxWidth(),
                            state = state,
                            currentLevel = currentLevel,
                            activeIssues = activeIssues,
                            onIntent = onIntent
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun HomeHeader(
    modifier: Modifier = Modifier,
    state: HomeState,
    currentLevel: org.example.project.core.model.home.PostLevel,
    activeIssues: Int,
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
                PostLevelChip(postLevel = currentLevel)

                Spacer(Modifier.width(spacing.small))

                Text(
                    text = "$activeIssues active issues",
                    style = IssueSpotTypography.bodyLarge,
                    color = IssueSpotColors.OnSurfaceVariant
                )
            }

            Spacer(Modifier.height(spacing.smallMedium))

            Text(
                text = "${currentLevel.displayName} Issues",
                style = IssueSpotTypography.bodyLarge,
                color = IssueSpotColors.OnSurface,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(spacing.extraSmall))

            Text(
                text = currentLevel.getText(),
                style = IssueSpotTypography.bodyLarge,
                color = IssueSpotColors.OnSurfaceVariant
            )
        }
    }
}
