package org.example.project.profile.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import org.example.project.home.presentation.screens.HomePullRefresh
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue


import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import org.example.project.core.components.CommentsBottomSheet
import android.content.Intent
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.runtime.key
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.platform.LocalContext

import androidx.compose.runtime.Composable
import org.example.project.core.components.AppErrorDialog
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import coil3.compose.AsyncImage
import androidx.core.net.toUri
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import org.example.project.R
import org.example.project.core.window.FeedConfig
import org.example.project.core.components.PostCard
import org.example.project.core.components.InfiniteScrollHandler
import org.example.project.core.components.PostLevelChip
import org.example.project.core.data.mappers.Sort
import org.example.project.core.model.home.Comment
import org.example.project.core.model.home.PostLevel
import org.example.project.core.model.profile.Profile
import org.example.project.core.presentation.FeedState
import org.example.project.feature.profile.viewmodel.ProfileIntent
import org.example.project.feature.profile.viewmodel.ProfileSideEffect
import org.example.project.feature.profile.viewmodel.ProfileState
import org.example.project.feature.profile.viewmodel.ProfileViewModel
import org.example.project.theme.IssueSpotColors
import org.example.project.theme.IssueSpotTheme
import org.example.project.theme.IssueSpotTypography
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.text.style.TextOverflow
import org.example.project.core.model.home.Post

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    onNavigateToCreatePost: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToPost: (postId: String) -> Unit ={},
    viewModel: ProfileViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val expandedPost = state.expandedPost
    val profilePostsState by viewModel.profilePostsState.collectAsStateWithLifecycle()
    var errorDialogMessage by remember { mutableStateOf<String?>(null) }


    val activeCommentsFlow by viewModel.activeCommentsFlow.collectAsStateWithLifecycle()
    val activePostId = state.showCommentsSheetForPostId

    val listState = rememberLazyListState()
    val showFab by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.sideEffects.collectLatest { effect ->
            when (effect) {
                ProfileSideEffect.NavigateToCreatePost -> onNavigateToCreatePost()
                ProfileSideEffect.NavigateToEditProfile -> onNavigateToEditProfile()
                is ProfileSideEffect.NavigateToPost -> onNavigateToPost(effect.postId)

                is ProfileSideEffect.ShowDialog -> {
                    errorDialogMessage = effect.message
                }
                is ProfileSideEffect.SharePost -> {
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, effect.text)
                        type = "text/plain"
                    }
                    val shareIntent = Intent.createChooser(sendIntent, "Share Issue via...")
                    context.startActivity(shareIntent)
                }
                is ProfileSideEffect.OpenMediaViewer -> {
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
        floatingActionButton = {
            if (showFab && expandedPost == null) {
                SmallFloatingActionButton(
                    onClick = { coroutineScope.launch { listState.animateScrollToItem(0) } },
                    containerColor = IssueSpotColors.Secondary,
                    contentColor = Color.White
                ) {
                    Text("↑", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
            }
        },
        containerColor = IssueSpotColors.Background
    ) { padding ->
        ProfileScreenContent(
            modifier = modifier.padding(padding),
            state = state,
            expandedPost = expandedPost,
            profilePostsState = profilePostsState,
            onIntent = viewModel::onIntent,
            listState = listState
        )
    }

    val currentCommentsFlow = activeCommentsFlow
    if (activePostId != null && currentCommentsFlow != null) {
        val commentsState by currentCommentsFlow.collectAsState()

        CommentsBottomSheet(
            comments = commentsState,
            onLoadMore = { viewModel.loadMoreComments(activePostId) },
            onDismiss = { viewModel.onIntent(ProfileIntent.DismissCommentsSheet) },
            onSubmit = { text ->
                viewModel.onIntent(
                    ProfileIntent.CommentSubmitted(
                        postId = activePostId,
                        commentText = text
                    )
                )
            },
            currentUserImageUrl = state.profile?.imageUrl
        )
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreenContent(
    modifier: Modifier = Modifier,
    state: ProfileState,
    expandedPost: Post?,
    profilePostsState: FeedState,
    onIntent: (ProfileIntent) -> Unit,
    listState: LazyListState
) {
    var postToDelete by remember { mutableStateOf<String?>(null) }
    val spacing = IssueSpotTheme.spacing
    val refreshError = profilePostsState.error
    val appendError = profilePostsState.appendError

    LaunchedEffect(refreshError) {
        if (refreshError != null && profilePostsState.posts.isNotEmpty()) {
            onIntent(ProfileIntent.ShowRefreshErrorDialog(refreshError.message))
        }
    }

    InfiniteScrollHandler(listState = listState) {
        onIntent(ProfileIntent.LoadMorePosts)
    }

    if (state.profile == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (state.isProfileLoading) {
                CircularProgressIndicator(color = IssueSpotColors.Primary)
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = state.profileError ?: "Error loading profile",
                        color = IssueSpotColors.OnBackground,
                        style = IssueSpotTypography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(spacing.medium))
                    Button(
                        onClick = { onIntent(ProfileIntent.RetryProfileClicked) },
                        colors = ButtonDefaults.buttonColors(containerColor = IssueSpotColors.Primary)
                    ) {
                        Text("Retry", color = Color.White)
                    }
                }
            }
        }
    } else {
        val profile = state.profile
        if (profile != null) {
            Box(modifier = modifier.fillMaxSize().background(IssueSpotColors.Background)) {
                if (expandedPost != null) {
                    val post = expandedPost

                    val isLiked = post.isLiked
                    val resolvedLikes = post.likes
                    val resolvedComments = post.comments
                    val isReported = post.isReported

                BackHandler {
                    onIntent(ProfileIntent.DismissPost)
                }

                PostCard(
                    modifier = Modifier.fillMaxSize(),
                    post = post,
                    isLiked = isLiked,
                    likesCount = resolvedLikes,
                    commentsCount = resolvedComments,
                    isReported = isReported,
                    canDelete = state.isMine,
                    canReport = !state.isMine,
                    onDeleteClick = { postToDelete = post.id },
                    onLikeClick = {
                        onIntent(ProfileIntent.LikeClicked(post.id))
                    },
                    onCommentIconClick = {
                        onIntent(ProfileIntent.CommentsIconClicked(post.id))
                    },
                    onShareClick = { onIntent(ProfileIntent.ShareClicked(post)) },
                    onReportClick = { reason ->
                        onIntent(ProfileIntent.ReportClicked(post.id, reason))
                    },
                    onCollapseClick = { onIntent(ProfileIntent.DismissPost) },
                    isDetailMode = true
                )
            } else {
                HomePullRefresh(
                    isRefreshing = profilePostsState.isRefreshing,
                    onRefresh = { onIntent(ProfileIntent.RefreshPosts) },
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = spacing.medium)
                    ) {
                    item { Spacer(Modifier.height(spacing.small)) }

                    item {
                        ProfileHeader(
                            imageUrl = profile.imageUrl,
                            name = profile.name,
                            location = profile.location,
                            totalPosts = profile.totalPosts,
                            acks = profile.acks,
                            onEditClick = { onIntent(ProfileIntent.EditProfileClicked) }
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    item {
                        Column {
                            Text(
                                text = "Posts by Area",
                                style = IssueSpotTypography.titleLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(spacing.extraSmall))
                            PostLevel.entries.forEachIndexed { i, entry ->
                                PostByAreaBar(
                                    postByArea = profile.postByArea.getOrElse(i) { 0 },
                                    postLevel = entry
                                )
                            }
                        }
                    }

                    item {
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onIntent(ProfileIntent.CreatePostClicked) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = IssueSpotColors.Primary,
                                contentColor = Color.White
                            ),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(
                                text = "+  Post New Issue",
                                style = IssueSpotTypography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    item {
                        ProfilePostTabsHeader(
                            isMine = state.isMine,
                            sort = state.sort,
                            onIntent = onIntent
                        )
                    }

                    if (profilePostsState.posts.isEmpty() && profilePostsState.isLoading) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(300.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = IssueSpotColors.Primary)
                            }
                        }
                    } else if (profilePostsState.posts.isEmpty() && refreshError != null) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp)
                                    .padding(spacing.medium),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = refreshError.message,
                                    color = IssueSpotColors.OnBackground,
                                    style = IssueSpotTypography.bodyMedium
                                )
                                Spacer(Modifier.height(spacing.small))
                                Button(
                                    onClick = { onIntent(ProfileIntent.RetryPosts) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = IssueSpotColors.Primary,
                                        contentColor = IssueSpotColors.OnPrimary
                                    )
                                ) {
                                    Text("Retry")
                                }
                            }
                        }
                    } else if (
                        profilePostsState.posts.isEmpty() && !profilePostsState.isRefreshing && profilePostsState.error == null
                    ) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp)
                                    .padding(spacing.huge),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No posts found",
                                    color = IssueSpotColors.OnBackground,
                                    style = IssueSpotTypography.bodyLarge
                                )
                            }
                        }
                    }

                    items(
                        items = profilePostsState.posts,
                        key = { post -> "${state.isMine}_${state.sort}_${post.id}" }
                    ) { post ->
                            val isLiked = post.isLiked
                            val resolvedLikes = post.likes
                            val resolvedComments = post.comments
                            val isReported = post.isReported

                            PostCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = spacing.smallMedium),
                                post = post,
                                isLiked = isLiked,
                                likesCount = resolvedLikes,
                                commentsCount = resolvedComments,
                                isReported = isReported,
                                canDelete = state.isMine,
                                canReport = !state.isMine,
                                onDeleteClick = { postToDelete = post.id },
                                onLikeClick = {
                                    onIntent(ProfileIntent.LikeClicked(post.id))
                                },
                                onCommentIconClick = {
                                    onIntent(ProfileIntent.CommentsIconClicked(post.id))
                                },
                                onShareClick = { onIntent(ProfileIntent.ShareClicked(post)) },
                                onReportClick = { reason -> 
                                    onIntent(ProfileIntent.ReportClicked(post.id, reason)) 
                                },
                                onPostClick = { onIntent(ProfileIntent.PostClicked(post)) }
                            )
                    }

                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 72.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                profilePostsState.isAppending -> {
                                    CircularProgressIndicator(color = IssueSpotColors.Primary)
                                }
                                appendError != null -> {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(spacing.medium),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = appendError.message,
                                            color = IssueSpotColors.OnBackground,
                                            style = IssueSpotTypography.bodyMedium
                                        )
                                        Spacer(Modifier.height(spacing.small))
                                        Button(
                                            onClick = { onIntent(ProfileIntent.RetryPosts) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = IssueSpotColors.Primary,
                                                contentColor = IssueSpotColors.OnPrimary
                                            )
                                        ) {
                                            Text("Retry")
                                        }
                                    }
                                }
                                !profilePostsState.hasMore && profilePostsState.posts.isNotEmpty() -> {
                                    Text(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(spacing.medium),
                                        text = "No more posts",
                                        color = IssueSpotColors.OnBackground,
                                        style = IssueSpotTypography.bodyMedium,
                                        textAlign = TextAlign.Center
                                    )
                                }
                                else -> {
                                    // Empty state, box remains 72dp
                                }
                            }
                        }
                    }
                    
                    item { Spacer(Modifier.height(spacing.medium)) }
                }
                }
            }
        }
        }
    }

    val currentPostToDelete = postToDelete
    if (currentPostToDelete != null) {
        AlertDialog(
            onDismissRequest = { postToDelete = null },
            title = { Text("Delete Post", style = IssueSpotTypography.titleMedium, fontWeight = FontWeight.Bold, color = IssueSpotColors.OnBackground) },
            text = { Text("Are you sure you want to delete this post? This action cannot be undone.", color = IssueSpotColors.OnSurfaceVariant, style = IssueSpotTypography.bodyMedium) },
            containerColor = IssueSpotColors.Surface,
            confirmButton = {
                TextButton(onClick = { 
                    onIntent(ProfileIntent.DeletePostClicked(currentPostToDelete)) 
                    postToDelete = null 
                }) { 
                    Text("Delete", color = Color.Red, fontWeight = FontWeight.Bold, style = IssueSpotTypography.labelLarge) 
                }
            },
            dismissButton = {
                TextButton(onClick = { postToDelete = null }) { 
                    Text("Cancel", color = IssueSpotColors.Primary, style = IssueSpotTypography.labelLarge) 
                }
            }
        )
    }


}

@Composable
private fun ProfileHeader(
    imageUrl: String?,
    name: String,
    location: String,
    totalPosts: Int,
    acks: Int,
    onEditClick: () -> Unit
) {
    val spacing = IssueSpotTheme.spacing
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(IssueSpotColors.Surface),
                contentAlignment = Alignment.Center
            ) {
                if (!imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = imageUrl.toUri(),
                        contentDescription = "${name}'s avatar",
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                        error = painterResource(R.drawable.ic_user_avatar),
                        fallback = painterResource(R.drawable.ic_user_avatar)
                    )
                } else {
                    Image(
                        painter = painterResource(R.drawable.ic_user_avatar),
                        contentDescription = "${name}'s avatar",
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Spacer(modifier = Modifier.width(spacing.small))
            Column (
                modifier = Modifier.weight(1f)
            ){
                Text(
                    text = name,
                    style = IssueSpotTypography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(spacing.extraSmall))
                Text(
                    text = location.ifBlank { "No location set" },
                    style = IssueSpotTypography.bodyMedium,
                    color = IssueSpotColors.OnSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                painter = painterResource(R.drawable.ic_edit),
                contentDescription = "Edit",
                modifier = Modifier
                    .clickable { onEditClick() }
                    .size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(spacing.medium))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatsCard(count = totalPosts, label = "Total Posts", modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(spacing.smallMedium))
            StatsCard(count = acks, label = "Acknowledgements", modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatsCard(count: Int, label: String, modifier: Modifier = Modifier) {
    val spacing = IssueSpotTheme.spacing
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = spacing.smallMedium),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                textAlign = TextAlign.Center,
                style = IssueSpotTypography.headlineMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            Text(
                text = label,
                textAlign = TextAlign.Center,
                style = IssueSpotTypography.labelSmall,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun PostByAreaBar(
    modifier: Modifier = Modifier,
    postByArea: Int,
    postLevel: PostLevel
) {
    val spacing = IssueSpotTheme.spacing
    Row(
        modifier = modifier
            .padding(spacing.extraSmall)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PostLevelChip(postLevel, modifier)
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "$postByArea posts",
            style = IssueSpotTypography.bodySmall,
        )
    }
}

@Composable
fun ProfilePostTabsHeader(
    isMine: Boolean,
    sort: Sort,
    onIntent: (ProfileIntent) -> Unit
) {
    val spacing = IssueSpotTheme.spacing
    Column {
        SegmentedControl(
            items = listOf("My Posts", "Liked Posts"),
            selectedIndex = if (isMine) 0 else 1,
            onItemSelected = { index ->
                onIntent(ProfileIntent.TabChanged(isMine = index == 0))
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(spacing.large))

        SegmentedControlFilter(
            items = listOf("Latest", "Oldest", "Popular"),
            selectedIndex = when (sort) {
                Sort.LATEST -> 0
                Sort.OLDEST -> 1
                Sort.POPULAR -> 2
            },
            onItemSelected = { index ->
                val sort = when (index) {
                    0 -> Sort.LATEST
                    1 -> Sort.OLDEST
                    2 -> Sort.POPULAR
                    else -> Sort.LATEST
                }
                onIntent(ProfileIntent.SortChanged(sort))
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(spacing.large))
    }
}

@Composable
fun SegmentedControl(
    items: List<String>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = IssueSpotTheme.spacing
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .height(36.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(spacing.extraSmall),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        items.forEachIndexed { index, title ->
            val isSelected = index == selectedIndex

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent
                    )
                    .clickable { onItemSelected(index) }
                    .padding(horizontal = spacing.small),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = IssueSpotTypography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun SegmentedControlFilter(
    items: List<String>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 32.dp,
    gap: Dp = 10.dp,
    cornerRadius: Dp = 12.dp
) {
    val shape = RoundedCornerShape(cornerRadius)
    val borderColor = MaterialTheme.colorScheme.outlineVariant

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEachIndexed { index, title ->
            val selected = index == selectedIndex

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(height)
                    .clip(shape)
                    .background(
                        color = if (selected) IssueSpotColors.OnBackground else MaterialTheme.colorScheme.surface
                    )
                    .border(0.4.dp, borderColor, shape)
                    .clickable { onItemSelected(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                    style = IssueSpotTypography.labelLarge,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}
