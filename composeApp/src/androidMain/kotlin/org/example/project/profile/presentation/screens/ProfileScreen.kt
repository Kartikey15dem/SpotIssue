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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.compose.collectAsLazyPagingItems
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
import org.example.project.theme.IssueSpotTypography
import org.koin.compose.viewmodel.koinViewModel

/**
 * ProfileScreen with ViewModel integration
 */
@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    onNavigateToCreatePost: () -> Unit = {},
    onNavigateToEditProfile: () -> Unit = {},
    onNavigateToPost: (postId: String) -> Unit = {},
    viewModel: ProfileViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle side effects
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
                    snackbarHostState.showSnackbar("Share: ${effect.text}")
                }
                is ProfileSideEffect.OpenMediaViewer -> {
                }
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
        ProfileScreenContent(
            modifier = modifier.padding(padding),
            state = state,
            onIntent = viewModel::onIntent
        )
    }
}

@Composable
fun ProfileScreenContent(
    modifier: Modifier = Modifier,
    state: ProfileState,
    onIntent: (ProfileIntent) -> Unit = {}
) {
//    val pagingItems = state.postsFlow?.collectAsLazyPagingItems()
//
//    LazyColumn(
//        modifier = modifier
//            .fillMaxSize()
//            .padding(16.dp),
//        verticalArrangement = Arrangement.spacedBy(12.dp)
//    ) {
//        item {
//            when (val res = state.profileState) {
//                is DataState.Success -> ProfileHeader(res.data, onIntent)
//                is DataState.Error -> Text("Error loading profile")
//                DataState.Loading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
//            }
//        }
//
//        item {
//            Column {
//                Text(
//                    text = "Posts by Area",
//                    style = IssueSpotTypography.bodyLarge,
//                    fontWeight = FontWeight.SemiBold
//                )
//                Spacer(modifier = Modifier.height(4.dp))
//                if (state.profileState is DataState.Success) {
//                    val profile = state.profileState.data
//                    PostLevel.entries.forEachIndexed { i, entry ->
//                        PostByAreaBar(
//                            postByArea = profile.postByArea.getOrElse(i) { 0 },
//                            postLevel = entry
//                        )
//                    }
//                }
//            }
//        }
//
//        item {
//            Button(
//                modifier = Modifier.fillMaxWidth(),
//                onClick = { onIntent(ProfileIntent.CreatePostClicked) },
//                colors = ButtonDefaults.buttonColors(
//                    containerColor = IssueSpotColors.PostButtonBackground,
//                    contentColor = IssueSpotColors.PostButtonText
//                )
//            ) {
//                Text(
//                    text = "+  Post New Issue",
//                    style = IssueSpotTypography.bodyLarge,
//                    fontWeight = FontWeight.Bold
//                )
//            }
//        }
//
//        item {
//            ProfilePostTabsHeader(state, onIntent)
//        }
//
//        if (pagingItems != null) {
//            items(
//                count = pagingItems.itemCount,
//                key = pagingItems.itemKey { it.id }
//            ) { index ->
//                val post = pagingItems[index]
//                if (post != null) {
//                    PostCard(
//                        modifier = Modifier.fillMaxWidth(),
//                        post = post,
//                        onLikeClick = { onIntent(ProfileIntent.LikeClicked(post.id)) },
//                        onCommentClick = { onIntent(ProfileIntent.CommentClicked(post.id)) },
//                        onShareClick = { onIntent(ProfileIntent.ShareClicked(post.id)) },
//                        onReportClick = { onIntent(ProfileIntent.ReportClicked(post.id)) },
//                        canDelete = state.isMine,
//                        onDeleteClick = { onIntent(ProfileIntent.DeletePostClicked(post.id)) }
//                    )
//                }
//            }
//        }
//    }
}

@Composable
private fun ProfileHeader(profile: Profile, onIntent: (ProfileIntent) -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(IssueSpotColors.Surface),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_user_avatar),
                    contentDescription = "${profile.name}'s avatar",
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(modifier = Modifier.padding(start = 8.dp))
            Column {
                Text(
                    text = profile.name,
                    style = IssueSpotTypography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "",//profile.location,
                    style = IssueSpotTypography.bodyMedium,
                    color = IssueSpotColors.OnSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                painter = painterResource(R.drawable.ic_edit),
                contentDescription = "Edit",
                modifier = Modifier
                    .clickable { onIntent(ProfileIntent.EditProfileClicked) }
                    .size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatsCard(count = profile.totalPosts, label = "Total Posts", modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(12.dp))
            StatsCard(count = profile.acks, label = "Acknowledgements", modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatsCard(count: Int, label: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                textAlign = TextAlign.Center,
                style = IssueSpotTypography.bodyLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            Text(
                text = label,
                textAlign = TextAlign.Center,
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
    Row(
        modifier = modifier
            .padding(4.dp)
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
    Column {
        SegmentedControl(
            items = listOf("My Posts", "Liked Posts"),
            selectedIndex = if (state.isMine) 0 else 1,
            onItemSelected = { index ->
                onIntent(ProfileIntent.TabChanged(isMine = index == 0))
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(20.dp))

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
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
fun SegmentedControl(
    items: List<String>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .height(32.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        items.forEachIndexed { index, title ->
            val isSelected = index == selectedIndex

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(ButtonDefaults.MinHeight)
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent
                    )
                    .clickable { onItemSelected(index) }
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
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
    height: Dp = 30.dp,
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
                        color = if (selected) Color(0xFF030213) else MaterialTheme.colorScheme.surface
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
