package org.example.project.home.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import kotlinx.coroutines.flow.collectLatest
import org.example.project.R
import org.example.project.core.utils.DataState
import org.example.project.home.domain.models.Post
import org.example.project.home.domain.models.PostLevel
import org.example.project.home.domain.models.getText
import org.example.project.home.presentation.components.PostCard
import org.example.project.home.presentation.components.PostLevelChip
import org.example.project.home.presentation.viewmodel.HomeIntent
import org.example.project.home.presentation.viewmodel.HomeSideEffect
import org.example.project.home.presentation.viewmodel.HomeState
import org.example.project.home.presentation.viewmodel.HomeViewModel
import org.example.project.theme.IssueSpotColors
import org.example.project.theme.IssueSpotTypography
import org.koin.compose.viewmodel.koinViewModel

/**
 * HomeScreen with ViewModel integration
 */
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onNavigateToCreatePost: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    viewModel: HomeViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

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
                    snackbarHostState.showSnackbar("Share: ${effect.text}")
                }
            }
        }
    }

    HomeContent(
        modifier = modifier,
        state = state,
        onIntent = viewModel::onIntent,
    )
}

/**
 * State-hoisted composable (pure UI).
 */
@Composable
fun HomeContent(
    modifier: Modifier = Modifier,
    state: HomeState,
    onIntent: (HomeIntent) -> Unit,
) {
    val pagingItems = state.postsFlow?.collectAsLazyPagingItems()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(IssueSpotColors.Background)
    ) {
        HomeHeader(
            modifier = Modifier.fillMaxWidth(),
            state = state,
            onIntent = onIntent
        )

        // Progress indicator for refresh state
        if (state.isRefreshing) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = IssueSpotColors.Primary,
                trackColor = IssueSpotColors.SurfaceVariant
            )
        }

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
                        PostCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp),
                            post = post,
                            onLikeClick = { onIntent(HomeIntent.LikeClicked(post.id)) },
                            onCommentClick = { onIntent(HomeIntent.CommentClicked(post.id, "")) },
                            onShareClick = { onIntent(HomeIntent.ShareClicked(post)) },
                            onReportClick = { onIntent(HomeIntent.ReportClicked(post.id)) },
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
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
