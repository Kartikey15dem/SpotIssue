package org.example.project.home.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import org.example.project.R
import org.example.project.core.components.CommentItem
import org.example.project.core.components.PostCard
import org.example.project.feature.home.viewmodel.PostDetailViewModel
import org.example.project.theme.IssueSpotColors
import org.example.project.theme.IssueSpotTypography
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    postId: String,
    onNavigateBack: () -> Unit,
    viewModel: PostDetailViewModel = koinViewModel(parameters = { parametersOf(postId) })
) {
    val state by viewModel.uiState.collectAsState()
    val comments = state.commentsFlow?.collectAsLazyPagingItems()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Post", style = IssueSpotTypography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_back),
                            contentDescription = "Back",
                            tint = IssueSpotColors.OnSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = IssueSpotColors.Background,
                    titleContentColor = IssueSpotColors.OnBackground
                )
            )
        },
        containerColor = IssueSpotColors.Background
    ) { paddingValues ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = IssueSpotColors.Primary)
            }
        } else if (state.error != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = state.error!!, color = IssueSpotColors.Error)
            }
        } else if (state.post != null) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                item {
                    PostCard(
                        post = state.post!!,
                        isLiked = false, // You might want to implement proper liked state logic later
                        likesCount = state.post!!.likes,
                        commentsCount = state.post!!.comments,
                        isReported = false,
                        onLikeClick = { },
                        onCommentIconClick = { },
                        onShareClick = { },
                        onReportClick = { }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Comments",
                        style = IssueSpotTypography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    HorizontalDivider(color = IssueSpotColors.Outline, thickness = 1.dp)
                }

                if (comments != null) {
                    items(comments.itemCount) { index ->
                        comments[index]?.let { comment ->
                            CommentItem(comment)
                        }
                    }
                }
            }
        }
    }
}