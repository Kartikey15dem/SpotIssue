package org.example.project.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import org.example.project.R
import org.example.project.core.model.home.Comment
import org.example.project.core.presentation.PaginationState
import org.example.project.theme.IssueSpotColors
import org.example.project.theme.IssueSpotTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsBottomSheet(
    comments: PaginationState<Comment>?,
    onLoadMore: () -> Unit,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
    /* WHY PASS CURRENT USER IMAGE URL:
     * We pass this from the ViewModel so that when the user submits a comment,
     * we can immediately inject it into the top of the PagingData stream (Optimistic UI).
     * This avoids a "flash" or waiting for the network response, making the app feel instant.
     */
    currentUserImageUrl: String? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var commentText by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()

    InfiniteScrollHandler(listState = listState, buffer = 3) {
        if (comments != null && comments.hasMore && !comments.isAppending && !comments.isLoading) {
            onLoadMore()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = IssueSpotColors.Surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f),
        ) {
            Text(
                text = "Comments",
                style = IssueSpotTypography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            HorizontalDivider(color = IssueSpotColors.SurfaceVariant)

            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
            ) {
                if (comments == null) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = IssueSpotColors.Primary,
                    )
                } else if (comments.items.isEmpty() && !comments.isLoading && !comments.isRefreshing) {
                    Text(
                        text = "No comments yet. Be the first to start the discussion!",
                        style = IssueSpotTypography.bodyLarge,
                        color = IssueSpotColors.OnSurfaceVariant,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .align(Alignment.Center)
                                .padding(horizontal = 16.dp),
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        items(comments.items.size) { index ->
                            comments.items.getOrNull(index)?.let { comment ->
                                CommentItem(comment)
                            }
                        }

                        if (comments.isAppending || comments.isLoading || comments.isRefreshing) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = IssueSpotColors.Primary)
                                }
                            }
                        } else if (!comments.hasMore && comments.items.isNotEmpty()) {
                            item {
                                Text(
                                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                                    text = "No more comments",
                                    color = IssueSpotColors.OnBackground,
                                    style = IssueSpotTypography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = IssueSpotColors.SurfaceVariant)

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(IssueSpotColors.Surface)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .navigationBarsPadding(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(IssueSpotColors.SurfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!currentUserImageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = currentUserImageUrl.toUri(),
                            contentDescription = "Your avatar",
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop,
                            error = painterResource(R.drawable.ic_user_avatar),
                            fallback = painterResource(R.drawable.ic_user_avatar),
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.ic_person),
                            contentDescription = "Your avatar",
                            modifier = Modifier.size(20.dp),
                            tint = IssueSpotColors.OnSurfaceVariant,
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    placeholder = { Text("Add a comment...") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 4,
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = IssueSpotColors.Primary,
                            unfocusedBorderColor = IssueSpotColors.SurfaceVariant,
                            focusedContainerColor = IssueSpotColors.SurfaceVariant.copy(alpha = 0.3f),
                            unfocusedContainerColor = IssueSpotColors.SurfaceVariant.copy(alpha = 0.3f),
                        ),
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (commentText.isNotBlank()) {
                            onSubmit(commentText)
                            commentText = ""
                        }
                    },
                    enabled = commentText.isNotBlank(),
                ) {
                    Icon(
                        painter = painterResource(android.R.drawable.ic_menu_send),
                        contentDescription = "Post Comment",
                        tint = if (commentText.isNotBlank()) IssueSpotColors.Primary else IssueSpotColors.OnSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun CommentItem(comment: Comment) {
    Row(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier =
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(IssueSpotColors.SurfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (!comment.userImageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = comment.userImageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop,
                    error = painterResource(R.drawable.ic_user_avatar),
                    fallback = painterResource(R.drawable.ic_user_avatar),
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = comment.userName,
                    fontWeight = FontWeight.Bold,
                    style = IssueSpotTypography.bodyMedium,
                    color = IssueSpotColors.OnSurface,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = comment.timeAgo,
                    color = IssueSpotColors.OnSurfaceVariant,
                    style = IssueSpotTypography.labelSmall,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = comment.text,
                style = IssueSpotTypography.bodyLarge,
                color = IssueSpotColors.OnSurface,
            )
        }
    }
}
