package org.example.project.home.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import org.example.project.theme.IssueSpotColors
import org.example.project.theme.IssueSpotTypography
import androidx.compose.ui.res.painterResource
import org.example.project.R
import androidx.compose.foundation.Image
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import org.example.project.core.components.ReportPostDialog
import org.example.project.core.model.home.MediaType
import org.example.project.core.model.home.Post
import org.example.project.core.model.home.PostLevel
import org.example.project.theme.IssueSpotTheme
import org.example.project.home.presentation.getColor

@Composable
fun PostCard(
    post: Post,
    isLiked: Boolean,
    likesCount: Int,
    commentsCount: Int,
    isReported: Boolean, // 👇 Add parameter
    modifier: Modifier = Modifier,
    canDelete: Boolean = false,
    onLikeClick: () -> Unit,
    onCommentClick: (String) -> Unit,
    onShareClick: () -> Unit,
    onReportClick: (String) -> Unit,
    onDeleteClick: () -> Unit = {},
) {
    var showCommentInput by rememberSaveable { mutableStateOf(false) }
    var commentText by rememberSaveable { mutableStateOf("") }
    var showReportDialog by rememberSaveable { mutableStateOf(false) }


    var fullscreenVisible by rememberSaveable { mutableStateOf(false) }



    // Estimate video aspect ratio
    val videoAspect = when {
        post.mediaUrl.contains("landscape", ignoreCase = true) -> 16f / 9f
        post.mediaUrl.contains("portrait", ignoreCase = true) -> 9f / 16f
        else -> 9f / 16f
    }

    Box {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = IssueSpotColors.CardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Box(Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Spacer(modifier = Modifier.height(16.dp))
                    PostHeader(
                        userName = post.userName,
                        timeAgo = post.timeAgo,
                        postLevel = post.postLevel,
                        location = ""
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = post.postText, style = IssueSpotTypography.bodyLarge)
                    Spacer(modifier = Modifier.height(12.dp))

                    when (post.mediaType) {
                        MediaType.IMAGE, MediaType.GIF -> {
                            Image(
                                painter = painterResource(R.drawable.img_post_placeholder),
                                contentDescription = "Post media",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                        MediaType.VIDEO -> { /* Implementation */ }
                        MediaType.PDF -> { /* Implementation */ }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(
                                id = if (isLiked) R.drawable.ic_like else R.drawable.ic_like
                            ),
                            contentDescription = "Like",
                            modifier = Modifier
                                .clickable { onLikeClick() }
                                .size(20.dp)
                                .padding(end = 2.dp),
                            tint = if (isLiked) IssueSpotColors.Primary else IssueSpotColors.OnSurfaceVariant
                        )
                        Text(
                            text = likesCount.toString(),
                            style = IssueSpotTypography.bodyLarge,
                            color = if (isLiked) IssueSpotColors.Primary else IssueSpotColors.OnSurfaceVariant
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        // Toggle comment input visibility
                        Icon(
                            painter = painterResource(R.drawable.ic_comment),
                            contentDescription = "Comment",
                            modifier = Modifier
                                .clickable { showCommentInput = !showCommentInput }
                                .size(20.dp)
                                .padding(end = 2.dp),
                            tint = IssueSpotColors.OnSurfaceVariant
                        )
                        Text(
                            text = commentsCount.toString(),
                            style = IssueSpotTypography.bodyLarge,
                            color = IssueSpotColors.OnSurfaceVariant
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        // Trigger report dialog
                        Icon(
                            painter = painterResource(R.drawable.ic_report),
                            contentDescription = "Report",
                            modifier = Modifier
                                .clickable(enabled = !isReported) {
                                    // Open dialog only if not reported yet
                                    showReportDialog = true
                                }
                                .size(20.dp)
                                .padding(end = 2.dp),
                            tint = if (isReported) {
                                IssueSpotColors.Error // Changes to red/orange on success
                            } else {
                                IssueSpotColors.OnSurfaceVariant // Gray by default
                            }
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Icon(
                            painter = painterResource(R.drawable.ic_share),
                            contentDescription = "Share",
                            modifier = Modifier
                                .clickable { onShareClick() }
                                .size(20.dp)
                                .padding(end = 2.dp)
                        )
                    }

                    // --- NEW: Comment Input Field ---
                    if (showCommentInput) {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = IssueSpotColors.SurfaceVariant, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = commentText,
                                onValueChange = { commentText = it },
                                placeholder = { Text("Add a comment...") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(24.dp),
                                maxLines = 3,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = IssueSpotColors.Primary,
                                    unfocusedBorderColor = IssueSpotColors.SurfaceVariant
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    if (commentText.isNotBlank()) {
                                        onCommentClick(commentText)
                                        commentText = "" // Clear after sending
                                        showCommentInput = false // Hide input optionally
                                    }
                                },
                                enabled = commentText.isNotBlank()
                            ) {
                                // Assuming you have an ic_send drawable
                                Icon(
                                    painter = painterResource(android.R.drawable.ic_menu_send),
                                    contentDescription = "Post Comment",
                                    tint = if (commentText.isNotBlank()) IssueSpotColors.Primary else IssueSpotColors.OnSurfaceVariant
                                )
                            }
                        }
                    }
                }
                if (canDelete) {
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-12).dp, y = 12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .size(36.dp)
                            .background(IssueSpotColors.Error),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_delete),
                            contentDescription = "Delete post",
                            modifier = Modifier.size(22.dp),
                            tint = IssueSpotColors.Surface
                        )
                    }
                }
            }
            if (showReportDialog) {
                ReportPostDialog(
                    onDismiss = { showReportDialog = false },
                    onSubmit = { reason ->
                        onReportClick(reason)
                        showReportDialog = false
                    }
                )
            }
        }

        // Overlay placed OUTSIDE Card so it covers entire screen
        if (fullscreenVisible) {
            FullscreenVideoOverlay(
                url = post.mediaUrl,
                videoAspectRatio = videoAspect,
                visible = fullscreenVisible,
                onDismiss = { fullscreenVisible = false }
            )
        }
    }
}

@Composable
private fun PostCardDeleteButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .size(36.dp), // button visual size (and touch target)
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
         )
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_delete),
            contentDescription = "Delete post",
            modifier = Modifier.size(22.dp), // bigger icon, but still inside the button
            tint = MaterialTheme.colorScheme.onErrorContainer
         )
    }
}

/**
 * Reusable post header component showing user avatar, name, time, post level, and location
 */
@Composable
fun PostHeader(
    userName: String,
    timeAgo: String? = null,
    postLevel: PostLevel,
    location: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(IssueSpotColors.SurfaceVariant),
            contentAlignment = Alignment.TopCenter
        ) {
            Image(
                painter = painterResource(R.drawable.ic_user_avatar),
                contentDescription = "$userName's avatar",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }
        Column {
            Row(
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp, start = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = userName,
                    style = IssueSpotTypography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                if (timeAgo != null) {
                    Text(
                        modifier = Modifier.padding(start = 8.dp),
                        text = timeAgo,
                        style = IssueSpotTypography.bodySmall,
                        color = IssueSpotColors.OnSurfaceVariant
                    )
                }
            }
            Row(
                modifier = Modifier.padding(start = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PostLevelChip(
                    postLevel = postLevel,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Icon(
                    painter = painterResource(R.drawable.ic_location_on),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = IssueSpotColors.OnSurfaceVariant
                )
                Text(
                    modifier = Modifier.padding(start = 4.dp),
                    text = location,
                    style = IssueSpotTypography.bodySmall,
                    color = IssueSpotColors.OnSurfaceVariant
                )
            }
        }
    }
}

/**
 * Reusable chip component for displaying post level with appropriate color
 */
@Composable
fun PostLevelChip(
    postLevel: PostLevel,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = postLevel.getColor().copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                BorderStroke(1.dp, postLevel.getColor().copy(alpha = 0.15f)),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Text(
            text = postLevel.displayName,
            color = postLevel.getColor(),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = IssueSpotTypography.bodySmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Preview
@Composable
fun PostCardPreview() {
    // Assuming a simple PostLevel enum or class for preview
    // If PostLevel is complex, this mock might need to be adjusted or defined elsewhere

    Column {
    IssueSpotTheme { // Apply your custom theme
        PostCard(
            post = samplePost,
            onLikeClick = {},
            onCommentClick = {},
            onShareClick = {},
            onReportClick = {},
            isLiked = true,
            likesCount = 34,
            commentsCount = 56,
            isReported = false,

            onDeleteClick = {}
        )
    }
    }
}
val samplePost = Post(
    id = "1",
    userName = "John Doe",
    userUrl = "https://example.com/avatar.jpg", // Replace with a real or placeholder image URL if needed for preview
//    location = "Downtown, Mumbai Central",
   // mediaUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4",
    mediaUrl = "https://gmcmasoonnpvjlvohzpt.supabase.co/storage/v1/object/public/vid/WhatsApp%20Video%202026-01-07%20at%2019.19.15.mp4",
    likes = 10,
    comments = 5,
    timeAgo ="14d ago",
    postLevel = PostLevel.LOCALITY,
    postText = "sfdksdkfhsdhafkhdsfkjh",
    mediaType = MediaType.VIDEO // Replace with the actual media type
)
