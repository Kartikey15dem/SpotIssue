package org.example.project.profile.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.flow.collectLatest
import org.example.project.home.domain.models.PostLevel
import org.example.project.home.presentation.components.PostCard
import org.example.project.home.presentation.components.PostLevelChip
import org.example.project.theme.IssueSpotColors
import org.example.project.theme.IssueSpotTheme
import org.example.project.theme.IssueSpotTypography
import org.example.project.profile.presentation.viewmodel.ProfileIntent
import org.example.project.profile.presentation.viewmodel.ProfileSideEffect
import org.example.project.profile.presentation.viewmodel.ProfileState
import org.example.project.profile.presentation.viewmodel.ProfileViewModel
import androidx.compose.ui.res.painterResource
import org.koin.compose.viewmodel.koinViewModel
import org.example.project.R
import org.example.project.profile.data.local.mapper.Sort

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
                    // Platform-specific share will be handled here
                    snackbarHostState.showSnackbar("Share: ${effect.text}")
                }
                is ProfileSideEffect.OpenMediaViewer -> {
                    // TODO: Open media viewer
                }
            }
        }
    }

    ProfileScreenContent(
        modifier = modifier,
        state = state,
        onIntent = viewModel::onIntent
    )
}
@Composable
fun ProfileScreenContent(
    modifier: Modifier = Modifier,
    state: ProfileState,
    onIntent: (ProfileIntent) -> Unit = {}
){
    Column(modifier = modifier.padding(16.dp)
        .fillMaxWidth())
    {
        // Progress indicator for refresh state
        if (state.isRefreshing) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = IssueSpotColors.Primary,
                trackColor = IssueSpotColors.SurfaceVariant
            )
        }

        Row( verticalAlignment = Alignment.CenterVertically){
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(IssueSpotColors.Surface),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_user_avatar),
                    contentDescription = "${state.profile.name}'s avatar",
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )

            }
            Spacer(modifier = Modifier.padding(start = 8.dp))
            Column {
                    Text(
                        text = state.profile.name,
                        style = IssueSpotTypography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                Spacer(modifier = Modifier.height(3.dp))
                    Text(

                        text = state.profile.location,
                        style = IssueSpotTypography.bodyMedium,
                        color = IssueSpotColors.OnSurfaceVariant
                    )
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                painter = painterResource(R.drawable.ic_edit),
                contentDescription = "Edit",
                modifier = Modifier.clickable { onIntent(ProfileIntent.EditProfileClicked) }
                    .size(20.dp)
                    .padding(end = 2.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Card(
                modifier = Modifier
                    .weight(1f)
            ) {
                Column(modifier = Modifier.fillMaxWidth()
                    .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = state.profile.totalPosts.toString(),
                        textAlign = TextAlign.Center,
                        style = IssueSpotTypography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    Text(
                        text = "Total Posts",
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp
                    )
                }

            }
            Spacer(modifier = Modifier.width(12.dp))
            Card(
                modifier = Modifier
                    .weight(1f)
            ) {
                Column(modifier = Modifier.fillMaxWidth()
                    .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = state.profile.acks.toString(),
                        textAlign = TextAlign.Center,
                        style = IssueSpotTypography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    Text(
                        text = "Acknowledgements",
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp
                    )
                }

            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Posts by Area",
            style = IssueSpotTypography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        var i = 0
        for(entry in PostLevel.entries){
            PostByAreaBar(
                postByArea = state.profile.postByArea[i],
                postLevel = entry
            )
            i++
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            modifier = Modifier.padding(1.dp)
                .fillMaxWidth(),
            onClick = { onIntent(ProfileIntent.CreatePostClicked) },
            colors = ButtonDefaults.buttonColors(
                containerColor = IssueSpotColors.PostButtonBackground,
                contentColor = IssueSpotColors.PostButtonText
            )
        ) {
            Text(
                text = "+  Post New Issue",
                style = IssueSpotTypography.bodyLarge,
                fontWeight = FontWeight.Bold

            )
        }
        Spacer(modifier = Modifier.height(6.dp))

       ProfilePostTabs(
           state = state,
           onIntent = onIntent
       )
    }
}
@Composable
fun PostByAreaBar(
    modifier: Modifier = Modifier,
    postByArea: Int,
    postLevel: PostLevel
){
    Row(modifier = modifier.padding(4.dp)
        .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ){
        PostLevelChip(postLevel,modifier)

        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "$postByArea posts",
            style = IssueSpotTypography.bodySmall,

        )

    }
}

@Composable
fun ProfilePostTabs(
    state: ProfileState,
    onIntent: (ProfileIntent) -> Unit
) {
    Column(
        modifier = Modifier
            .padding(8.dp)
    ) {

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

        // Display posts from state
        state.posts.forEach { post ->
            PostCard(
                modifier = Modifier.fillMaxWidth(),
                post = post,
                onLikeClick = { onIntent(ProfileIntent.LikeClicked(post.id)) },
                onCommentClick = { onIntent(ProfileIntent.CommentClicked(post.id)) },
                onShareClick = { onIntent(ProfileIntent.ShareClicked(post.id)) },
                onReportClick = { onIntent(ProfileIntent.ReportClicked(post.id)) },
                canDelete = state.isMine,
                onDeleteClick = { onIntent(ProfileIntent.DeletePostClicked(post.id)) }
            )
            Spacer(Modifier.height(12.dp))
        }
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
    height: Dp = 30.dp,          // closer to typical button height look
    gap: Dp = 10.dp,             // visible gap between chips (adjust to taste)
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


@Preview
@Composable
fun ProfileScreenPreview() {
    IssueSpotTheme {
        ProfileScreenContent(
            state = ProfileState()
        )
    }
}
