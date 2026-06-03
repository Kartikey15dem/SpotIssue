package org.example.project.home.presentation.screens

import android.content.Intent
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.paging.LoadState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import org.example.project.core.utils.DataState
import org.example.project.home.presentation.components.PostCard
import org.example.project.home.presentation.components.PostLevelChip
import org.example.project.home.presentation.viewmodel.HomeIntent
import org.example.project.home.presentation.viewmodel.HomeSideEffect
import org.example.project.home.presentation.viewmodel.HomeState
import org.example.project.home.presentation.viewmodel.HomeViewModel
import org.example.project.theme.IssueSpotColors
import org.example.project.theme.IssueSpotTypography
import org.example.project.core.components.CommentsBottomSheet
import org.koin.compose.viewmodel.koinViewModel

/**
 * HomeScreen with ViewModel integration
 */
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onNavigateToCreatePost: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToPost: (String) -> Unit = {},
    viewModel: HomeViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Handle side effects
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
                is HomeSideEffect.NavigateToPost -> onNavigateToPost(effect.postId)
            }
        }
    }

    Scaffold(
        snackbarHost = { 
            SnackbarHost(snackbarHostState) { data ->
                androidx.compose.material3.Snackbar(
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
            onNavigateToPost = onNavigateToPost
        )
    }
}

/**
 * State-hoisted composable (pure UI).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(
    modifier: Modifier = Modifier,
    state: HomeState,
    onIntent: (HomeIntent) -> Unit,
    onNavigateToPost: (String) -> Unit = {}
) {
    val activeFlow = if (state.query.isNotBlank() && state.searchPostsFlow != null) state.searchPostsFlow else state.postsFlow
    val pagingItems = activeFlow?.collectAsLazyPagingItems()
    val isRefreshing = pagingItems?.loadState?.refresh is LoadState.Loading || state.isRefreshing

    LaunchedEffect(pagingItems?.loadState?.refresh) {
        if (pagingItems?.loadState?.refresh is LoadState.Error) {
            val errorState = pagingItems.loadState.refresh as LoadState.Error
            // Handle error if needed
        }
    }

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
            HomeHeader(
                modifier = Modifier.fillMaxWidth(),
                state = state,
                onIntent = onIntent
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(Modifier.height(8.dp)) }

                if (pagingItems != null) {
                    items(
                        count = pagingItems.itemCount,
                        key = pagingItems.itemKey { it.id }
                    ) { index ->
                        val post = pagingItems[index]
                        if (post != null) {
                            val override = state.postOverrides[post.id]

                            val isLiked = override?.isLiked ?: false
                            val resolvedLikes = override?.likesCount ?: post.likes
                            val resolvedComments = override?.commentsCount ?: post.comments
                            val isReported = override?.isReported ?: false

                            PostCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp),
                                post = post,
                                isLiked = isLiked,
                                likesCount = resolvedLikes,
                                commentsCount = resolvedComments,
                                isReported = isReported,

                                onLikeClick = {
                                    onIntent(HomeIntent.LikeClicked(post.id, isLiked, resolvedLikes))
                                },
                                // 👇 Only pass the Intent, remove local states from PostCard!
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

                item { Spacer(Modifier.height(16.dp)) }

                if (pagingItems?.loadState?.refresh is LoadState.Loading && pagingItems.itemCount == 0) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            androidx.compose.material3.CircularProgressIndicator(color = IssueSpotColors.Primary)
                        }
                    }
                }
            }
        }
    }

    // 👇 Render Bottom Sheet at the screen level based on ViewModel state
    if (state.showCommentsSheetForPostId != null) {
        val activePostId = state.showCommentsSheetForPostId
        val activeOverride = state.postOverrides[activePostId]
        val commentsPagingItems = activeOverride?.commentsFlow?.collectAsLazyPagingItems()

        CommentsBottomSheet(
            comments = commentsPagingItems,
            onDismiss = { onIntent(HomeIntent.DismissCommentsSheet) },
            onSubmit = { text ->
                onIntent(
                    HomeIntent.CommentSubmitted(
                        postId = activePostId,
                        commentText = text,
                        currentCommentCount = activeOverride?.commentsCount ?: 0 // Fetch actual count to bump it
                    )
                )
            }
        )
    }
}

@Composable
private fun HomeHeader(
    modifier: Modifier = Modifier,
    state: HomeState,
    onIntent: (HomeIntent) -> Unit,
) {
    Column(
        modifier = modifier
            .background(IssueSpotColors.Surface)
            .padding(horizontal = 12.dp, vertical = 10.dp)
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
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = IssueSpotColors.SurfaceVariant,
                    focusedContainerColor = IssueSpotColors.SurfaceVariant,
                    unfocusedBorderColor = IssueSpotColors.SurfaceVariant,
                    focusedBorderColor = IssueSpotColors.SurfaceVariant,
                    unfocusedTextColor = IssueSpotColors.OnSurface,
                    focusedTextColor = IssueSpotColors.OnSurface,
                )
            )

            Spacer(Modifier.width(6.dp))

            Button(
                onClick = { onIntent(HomeIntent.CreatePostClicked) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = IssueSpotColors.PostButtonBackground,
                    contentColor = IssueSpotColors.PostButtonText
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.defaultMinSize(minWidth = 0.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Post", style = IssueSpotTypography.bodyLarge)
            }

            Spacer(Modifier.width(6.dp))

            IconButton(onClick = { onIntent(HomeIntent.ProfileClicked) }) {
                Icon(
                    painter = painterResource(R.drawable.ic_person),
                    contentDescription = "Profile",
                    tint = IssueSpotColors.OnSurface
                )
            }
        }

        if (state.query.isBlank()) {
            Spacer(Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {

                PostLevelChip(postLevel = state.postLevel)

                Spacer(Modifier.width(10.dp))

                val activeIssuesText = when (val res = state.activeIssues) {
                    is DataState.Success -> "${res.data} active issues"
                    is DataState.Error -> "Error loading issues"
                    DataState.Loading -> "Loading issues..."
                }

                Text(
                    text = "  $activeIssuesText".replace("\u0005", "↗"),
                    style = IssueSpotTypography.bodyLarge,
                    color = IssueSpotColors.OnSurfaceVariant
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = "${state.postLevel.displayName} Issues",
                style = IssueSpotTypography.bodyLarge,
                color = IssueSpotColors.OnSurface,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = state.postLevel.getText(),
                style = IssueSpotTypography.bodyLarge,
                color = IssueSpotColors.OnSurfaceVariant
            )
        }
    }
}
