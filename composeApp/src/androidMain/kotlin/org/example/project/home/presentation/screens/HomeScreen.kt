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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import org.example.project.core.components.AppErrorDialog
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    onExpandedPostChange: (Boolean) -> Unit = {},
    viewModel: HomeViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentLevel by viewModel.currentLevel.collectAsStateWithLifecycle()
    val activeIssues by viewModel.activeIssues.collectAsStateWithLifecycle()
    val expandedPost by viewModel.expandedPost.collectAsStateWithLifecycle()
    
    val feedState by viewModel.feedState.collectAsStateWithLifecycle()
    val searchState by viewModel.searchState.collectAsStateWithLifecycle()
    
    val activeCommentsFlow by viewModel.activeCommentsFlow.collectAsStateWithLifecycle()
    val activePostId = state.showCommentsSheetForPostId
    
    var errorDialogMessage by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current

    LaunchedEffect(expandedPost) {
        onExpandedPostChange(expandedPost != null)
    }
    LaunchedEffect(currentLevel) {
    }
    LaunchedEffect(activeCommentsFlow) {
    }

    LaunchedEffect(viewModel) {
        viewModel.sideEffects.collectLatest { effect ->
            when (effect) {
                is HomeSideEffect.NavigateToCreatePost -> onNavigateToCreatePost()
                is HomeSideEffect.NavigateToProfile -> onNavigateToProfile()
                is HomeSideEffect.ShowError -> {
                    errorDialogMessage = effect.message
                }
                is HomeSideEffect.ShowDialog -> {
                    errorDialogMessage = effect.message
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

    
    errorDialogMessage?.let { message ->
        AppErrorDialog(
            message = message,
            onDismiss = { errorDialogMessage = null }
        )
    }

    Scaffold(
        containerColor = IssueSpotColors.Background
    ) { padding ->
        HomeContent(
            modifier = modifier.padding(padding),
            state = state,
            currentLevel = currentLevel,
            activeIssues = activeIssues,
            expandedPost = expandedPost,
            feedState = feedState,
            searchState = searchState,
            onIntent = viewModel::onIntent,
        )
    }

    val currentCommentsFlow = activeCommentsFlow
    if (activePostId != null && currentCommentsFlow != null) {
        val commentsState by currentCommentsFlow.collectAsState()
        
        CommentsBottomSheet(
            comments = commentsState,
            onLoadMore = { viewModel.loadMoreComments(activePostId) },
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
    feedState: org.example.project.core.presentation.FeedState,
): FeedUiState {
    val itemCount = feedState.posts.size
    val showInitialLoading = itemCount == 0 && feedState.isLoading
    val showInitialError = itemCount == 0 && feedState.error != null && !feedState.isLoading
    val showEmptyFeed = itemCount == 0 && !feedState.isLoading && feedState.error == null && !feedState.isRefreshing
    val showFeed = itemCount > 0
    
    val isPullRefreshing = feedState.isRefreshing && itemCount > 0

    val appendError = feedState.appendError
    val shouldShowFooter = itemCount > 0 && !feedState.isRefreshing
    val showNoMorePosts = shouldShowFooter && !feedState.hasMore && appendError == null && !feedState.isAppending

    val footerState = when {
        !shouldShowFooter -> FooterState.Hidden
        feedState.isAppending -> FooterState.Loading
        appendError != null -> FooterState.Error(Throwable(appendError.message))
        showNoMorePosts -> FooterState.EndReached
        else -> FooterState.Hidden
    }

    return FeedUiState(
        showInitialLoading = showInitialLoading,
        showFeed = showFeed,
        showEmptyFeed = showEmptyFeed,
        showInitialError = showInitialError,
        footerState = footerState,
        refreshError = if (feedState.error != null) Throwable(feedState.error?.message) else null,
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
    feedState: org.example.project.core.presentation.FeedState,
    searchState: org.example.project.core.presentation.FeedState,
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
    
    val feedUiState = rememberFeedUiState(feedState)

    LaunchedEffect(feedUiState.refreshError) {
        if (feedUiState.refreshError != null && feedState.posts.isNotEmpty()) {
            onIntent(HomeIntent.ShowRefreshErrorDialog(feedUiState.refreshError.message ?: "An error occurred"))
        }
    }

    LaunchedEffect(feedUiState.isPullRefreshing) {
    }

    HomePullRefresh(
        isRefreshing = feedUiState.isPullRefreshing,
        onRefresh = { onIntent(HomeIntent.RefreshPosts()) },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            if (feedState.isOffline) {
                HomeOfflineBanner()
            }
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
                HomeHeader(
                    modifier = Modifier.fillMaxWidth(),
                    state = state,
                    currentLevel = currentLevel,
                    activeIssues = activeIssues,
                    onIntent = onIntent
                )
                
                if (state.query.isNotBlank()) {
                    HomeSearchFeed(
                        searchState = searchState,
                        onIntent = onIntent,
                        modifier = Modifier.fillMaxSize(),
                        header = {}
                    )
                } else {
                    HomeFeed(
                        listState = listState,
                        feedState = feedState,
                        onIntent = onIntent,
                        feedUiState = feedUiState,
                        modifier = Modifier.fillMaxSize(),
                        header = {}
                    )
                }
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
