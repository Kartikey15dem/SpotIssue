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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.activity.compose.BackHandler
import androidx.compose.material3.IconButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue


import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import org.example.project.core.components.CommentsBottomSheet
import android.content.Intent
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Snackbar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.LoadState
import androidx.paging.compose.itemKey
import kotlinx.coroutines.flow.collectLatest
import org.example.project.R
import org.example.project.core.utils.DataState
import org.example.project.home.presentation.components.PostCard
import org.example.project.home.presentation.components.PostLevelChip
import org.example.project.core.data.mappers.Sort
import org.example.project.core.model.home.PostLevel
import org.example.project.core.model.profile.Profile
import org.example.project.profile.presentation.viewmodel.ProfileIntent
import org.example.project.profile.presentation.viewmodel.ProfileSideEffect
import org.example.project.profile.presentation.viewmodel.ProfileState
import org.example.project.profile.presentation.viewmodel.ProfileViewModel
import org.example.project.theme.IssueSpotColors
import org.example.project.theme.IssueSpotTheme
import org.example.project.theme.IssueSpotTypography
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    onNavigateToCreatePost: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToPost: (postId: String) -> Unit ={},
    viewModel: ProfileViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

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
                is ProfileSideEffect.ShowError -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
                is ProfileSideEffect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(effect.message)
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
        floatingActionButton = {
            if (showFab && state.expandedPost == null) {
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
            onIntent = viewModel::onIntent,
            listState = listState
        )
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreenContent(
    modifier: Modifier = Modifier,
    state: ProfileState,
    onIntent: (ProfileIntent) -> Unit,
    listState: LazyListState
) {
    val pagingItems = state.activePostsFlow?.collectAsLazyPagingItems()
    var postToDelete by remember { mutableStateOf<String?>(null) }
    val spacing = IssueSpotTheme.spacing

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
        Box(modifier = modifier.fillMaxSize().background(IssueSpotColors.Background)) {
            if (state.expandedPost != null) {
                val post = state.expandedPost
                val override = state.postOverrides[post.id]

                val isLiked = override?.isLiked ?: post.isLiked
                val resolvedLikes = override?.likesCount ?: post.likes
                val resolvedComments = override?.commentsCount ?: post.comments
                val isReported = override?.isReported ?: post.isReported

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
                    onDeleteClick = { postToDelete = post.id },
                    onLikeClick = {
                        onIntent(ProfileIntent.LikeClicked(post.id, isLiked, resolvedLikes))
                    },
                    onCommentIconClick = {
                        onIntent(ProfileIntent.CommentsIconClicked(post.id, resolvedComments))
                    },
                    onShareClick = { onIntent(ProfileIntent.ShareClicked(post)) },
                    onReportClick = { reason ->
                        onIntent(ProfileIntent.ReportClicked(post.id, reason))
                    },
                    onCollapseClick = { onIntent(ProfileIntent.DismissPost) },
                    isDetailMode = true
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = spacing.medium),
                    verticalArrangement = Arrangement.spacedBy(spacing.smallMedium)
                ) {
                    item { Spacer(Modifier.height(spacing.small)) }

                    item {
                        ProfileHeader(state.profile, onIntent)
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
                                    postByArea = state.profile.postByArea.getOrElse(i) { 0 },
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
                        ProfilePostTabsHeader(state, onIntent)
                    }

                    if (pagingItems != null) {
                        val refreshError = pagingItems.loadState.refresh as? LoadState.Error
                        val appendError = pagingItems.loadState.append as? LoadState.Error

                        if (refreshError != null && pagingItems.itemCount == 0) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(spacing.huge),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = refreshError.error.message ?: "An error occurred",
                                        color = IssueSpotColors.OnBackground,
                                        style = IssueSpotTypography.bodyMedium
                                    )
                                }
                            }
                        } else if (pagingItems.loadState.refresh is LoadState.Loading && pagingItems.itemCount == 0) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = IssueSpotColors.Primary)
                                }
                            }
                        } else if (pagingItems.loadState.refresh is LoadState.NotLoading && pagingItems.itemCount == 0) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
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
                        } else {
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
                                        modifier = Modifier.fillMaxWidth(),
                                        post = post,
                                        isLiked = isLiked,
                                        likesCount = resolvedLikes,
                                        commentsCount = resolvedComments,
                                        isReported = isReported,
                                        canDelete = state.isMine,
                                        onDeleteClick = { postToDelete = post.id },
                                        onLikeClick = {
                                            onIntent(ProfileIntent.LikeClicked(post.id, isLiked, resolvedLikes))
                                        },
                                        onCommentIconClick = {
                                            onIntent(ProfileIntent.CommentsIconClicked(post.id, resolvedComments))
                                        },
                                        onShareClick = { onIntent(ProfileIntent.ShareClicked(post)) },
                                        onReportClick = { reason -> 
                                            onIntent(ProfileIntent.ReportClicked(post.id, reason)) 
                                        },
                                        onPostClick = { onIntent(ProfileIntent.PostClicked(post)) }
                                    )
                                }
                            }
                        }

                        if (pagingItems.loadState.append is LoadState.Loading) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(spacing.medium), contentAlignment = Alignment.Center) {
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
                    
                    item { Spacer(Modifier.height(spacing.medium)) }
                }
            }
        }
    }

    if (postToDelete != null) {
        AlertDialog(
            onDismissRequest = { postToDelete = null },
            title = { Text("Delete Post", style = IssueSpotTypography.titleMedium, fontWeight = FontWeight.Bold, color = IssueSpotColors.OnBackground) },
            text = { Text("Are you sure you want to delete this post? This action cannot be undone.", color = IssueSpotColors.OnSurfaceVariant, style = IssueSpotTypography.bodyMedium) },
            containerColor = IssueSpotColors.Surface,
            confirmButton = {
                TextButton(onClick = { 
                    onIntent(ProfileIntent.DeletePostClicked(postToDelete!!)) 
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

    if (state.showCommentsSheetForPostId != null) {
        val activePostId = state.showCommentsSheetForPostId
        val activeOverride = state.postOverrides[activePostId]
        val commentsPagingItems = activeOverride?.commentsFlow?.collectAsLazyPagingItems()

        CommentsBottomSheet(
            comments = commentsPagingItems,
            onDismiss = { onIntent(ProfileIntent.DismissCommentsSheet) },
            onSubmit = { text ->
                val fallbackCount = pagingItems?.itemSnapshotList?.items?.find { it.id == activePostId }?.comments ?: 0
                onIntent(
                    ProfileIntent.CommentSubmitted(
                        postId = activePostId,
                        commentText = text,
                        currentCommentCount = activeOverride?.commentsCount ?: fallbackCount
                    )
                )
            },
            currentUserImageUrl = state.profile?.imageUrl
        )
    }
}

@Composable
private fun ProfileHeader(profile: Profile, onIntent: (ProfileIntent) -> Unit) {
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
                val imageUrl = profile.imageUrl
                if (!imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = imageUrl.toUri(),
                        contentDescription = "${profile.name}'s avatar",
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
                        contentDescription = "${profile.name}'s avatar",
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
                    text = profile.name,
                    style = IssueSpotTypography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(spacing.extraSmall))
                Text(
                    text = profile.location.ifBlank { "No location set" },
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
                    .clickable { onIntent(ProfileIntent.EditProfileClicked) }
                    .size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(spacing.medium))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatsCard(count = profile.totalPosts, label = "Total Posts", modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(spacing.smallMedium))
            StatsCard(count = profile.acks, label = "Acknowledgements", modifier = Modifier.weight(1f))
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
    state: ProfileState,
    onIntent: (ProfileIntent) -> Unit
) {
    val spacing = IssueSpotTheme.spacing
    Column {
        SegmentedControl(
            items = listOf("My Posts", "Liked Posts"),
            selectedIndex = if (state.isMine) 0 else 1,
            onItemSelected = { index ->
                onIntent(ProfileIntent.TabChanged(isMine = index == 0))
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(spacing.large))

        SegmentedControlFilter(
            items = listOf("Latest", "Oldest", "Popular"),
            selectedIndex = when (state.sort) {
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
