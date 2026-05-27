package org.example.project.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.example.project.R
import org.example.project.core.model.home.Comment
import org.example.project.theme.IssueSpotColors
import org.example.project.theme.IssueSpotTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsBottomSheet(
    comments: List<Comment>?,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var commentText by rememberSaveable { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = IssueSpotColors.Surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        // Ensure the sheet can resize itself when the keyboard opens
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f) // Take up 85% of the screen height
        ) {
            // Header
            Text(
                text = "Comments",
                style = IssueSpotTypography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            HorizontalDivider(color = IssueSpotColors.SurfaceVariant)

            // Comments List (Takes up remaining space)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (isLoading && comments == null) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = IssueSpotColors.Primary
                    )
                } else if (comments.isNullOrEmpty()) {
                    Text(
                        text = "No comments yet. Be the first to start the discussion!",
                        style = IssueSpotTypography.bodyLarge,
                        color = IssueSpotColors.OnSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(comments, key = { it.id }) { comment ->
                            CommentItem(comment)
                        }
                    }
                }
            }

            HorizontalDivider(color = IssueSpotColors.SurfaceVariant)

            // Bottom Input Area (Sticky)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(IssueSpotColors.Surface)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .navigationBarsPadding(), // Handles bottom gesture bar spacing
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Current User Avatar
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(IssueSpotColors.SurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_person),
                        contentDescription = "Your avatar",
                        modifier = Modifier.size(20.dp),
                        tint = IssueSpotColors.OnSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Input Field
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    placeholder = { Text("Add a comment...") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = IssueSpotColors.Primary,
                        unfocusedBorderColor = IssueSpotColors.SurfaceVariant,
                        focusedContainerColor = IssueSpotColors.SurfaceVariant.copy(alpha = 0.3f),
                        unfocusedContainerColor = IssueSpotColors.SurfaceVariant.copy(alpha = 0.3f)
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Send Button
                IconButton(
                    onClick = {
                        if (commentText.isNotBlank()) {
                            onSubmit(commentText)
                            commentText = ""
                        }
                    },
                    enabled = commentText.isNotBlank()
                ) {
                    Icon(
                        painter = painterResource(android.R.drawable.ic_menu_send),
                        contentDescription = "Post Comment",
                        tint = if (commentText.isNotBlank()) IssueSpotColors.Primary else IssueSpotColors.OnSurfaceVariant
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
        // User Avatar
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(IssueSpotColors.SurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_person), // Fallback image
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = IssueSpotColors.OnSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Comment Content
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = comment.userName,
                    fontWeight = FontWeight.Bold,
                    style = IssueSpotTypography.bodyMedium,
                    color = IssueSpotColors.OnSurface
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = comment.timeAgo,
                    color = IssueSpotColors.OnSurfaceVariant,
                    style = IssueSpotTypography.labelSmall
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = comment.text,
                style = IssueSpotTypography.bodyLarge,
                color = IssueSpotColors.OnSurface
            )
        }
    }
}