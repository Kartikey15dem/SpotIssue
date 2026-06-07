package org.example.project.home.presentation.components

import android.provider.CalendarContract
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import org.example.project.theme.IssueSpotColors
import org.example.project.theme.IssueSpotTypography
import androidx.compose.ui.res.painterResource
import org.example.project.R
import androidx.compose.foundation.Image
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import org.example.project.core.components.ReportPostDialog
import org.example.project.core.components.LocalOverlayController
import org.example.project.core.components.PdfPreviewContent
import org.example.project.core.components.VideoPreviewPlayer
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
    isReported: Boolean,
    modifier: Modifier = Modifier,
    canDelete: Boolean = false,
    onLikeClick: () -> Unit,
    onCommentIconClick: () -> Unit,
    onShareClick: () -> Unit,
    onReportClick: (String) -> Unit,
    onDeleteClick: () -> Unit = {},
    onPostClick: () -> Unit = {},
    onCollapseClick: () -> Unit = {},
    isExpanded: Boolean = false
) {
    var showReportDialog by rememberSaveable { mutableStateOf(false) }
    var hasOverflow by remember { mutableStateOf(false) }

    Box {
        Card(
            modifier = modifier
                .then(
                    if (isExpanded) {
                        Modifier.fillMaxSize()
                    } else {
                        Modifier.fillMaxWidth()
                    }
                ),
            colors = CardDefaults.cardColors(containerColor = IssueSpotColors.CardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Box(Modifier.fillMaxSize()) {
                Column(modifier = Modifier.padding(16.dp)
                    .fillMaxSize()) {

                    Column(
                        modifier = Modifier.clickable {
                            if (!isExpanded) {
                                onPostClick()
                            }
                        }
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))

                        PostHeader(
                            userName = post.userName,
                            timeAgo = post.timeAgo,
                            postLevel = post.postLevel,
                            location = post.locality + "," +
                                    post.district + "," +
                                    post.state + "," +
                                    post.country,
                            isExpanded = isExpanded,
                            onCollapseClick = onCollapseClick
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = post.postText,
                            style = IssueSpotTypography.bodyLarge,
                            maxLines = if (isExpanded) Int.MAX_VALUE else 4,
                            overflow = TextOverflow.Ellipsis,
                            onTextLayout = {
                                hasOverflow = it.hasVisualOverflow
                            }
                        )

                        if (!isExpanded && hasOverflow) {
                            Text(
                                text = "More",
                                style = IssueSpotTypography.bodyMedium,
                                color = IssueSpotColors.Primary,
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .clickable {
                                        onPostClick()
                                    }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (!post.mediaUrls.isNullOrEmpty()) {
                        PostMediaPreview(post)
                    }
                    Spacer(modifier = Modifier.weight(1f))

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
                            tint = if (isLiked) IssueSpotColors.LikeActiveColor else IssueSpotColors.OnSurfaceVariant
                        )
                        Text(
                            text = likesCount.toString(),
                            style = IssueSpotTypography.bodyLarge,
                            color = if (isLiked) IssueSpotColors.LikeActiveColor else IssueSpotColors.OnSurfaceVariant
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Icon(
                            painter = painterResource(R.drawable.ic_comment),
                            contentDescription = "Comment",
                            modifier = Modifier
                                .clickable { onCommentIconClick() }
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

                        Icon(
                            painter = painterResource(
                                id = if (isReported) R.drawable.ic_report_filled else R.drawable.ic_report
                            ),
                            contentDescription = "Report",
                            modifier = Modifier
                                .clickable(enabled = !isReported) {
                                    showReportDialog = true
                                }
                                .size(20.dp)
                                .padding(end = 2.dp),
                            tint = if (isReported) IssueSpotColors.Error else IssueSpotColors.OnSurfaceVariant
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

    }
}

@Composable
fun PostHeader(
    userName: String,
    timeAgo: String? = null,
    postLevel: PostLevel,
    location: String,
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false,
    onCollapseClick: () -> Unit = {}
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isExpanded) {
            IconButton(
                onClick = onCollapseClick,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .size(24.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = "Close",
                    tint = IssueSpotColors.OnBackground
                )
            }
        }
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
            onCommentIconClick = {},
            onShareClick = {},
            onReportClick = {},
            isLiked = true,
            likesCount = 34,
            commentsCount = 56,
            isReported = false,

            onDeleteClick = {},

        )
    }
    }
}
val samplePost = Post(
    id = "1",
    userName = "John Doe",
    userUrl = "https://example.com/avatar.jpg", // Replace with a real or placeholder image URL if needed for preview
//    location = "Downtown, Mumbai Central",
   // mediaUrls = listOf("https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4"),
    mediaUrls = listOf("https://gmcmasoonnpvjlvohzpt.supabase.co/storage/v1/object/public/vid/WhatsApp%20Video%202026-01-07%20at%2019.19.15.mp4"),
    likes = 10,
    comments = 5,
    timeAgo ="14d ago",
    postLevel = PostLevel.LOCALITY,
    postText = "sfdksdkfhsdhafkhdsfkjh",
    mediaType = MediaType.VIDEO // Replace with the actual media type
)

@Composable
fun PostMediaPreview(
    post: Post,
    modifier: Modifier = Modifier
) {
    val overlayController = LocalOverlayController.current
    var aspectRatio by remember { mutableFloatStateOf(1f) }
    
    val mediaUrls = post.mediaUrls ?: return

    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
                .clip(RoundedCornerShape(8.dp))
        ) {
            when (post.mediaType) {
                MediaType.IMAGE -> {
                    PostImageGrid(
                        images = mediaUrls,
                        onImageClick = { clickedIndex ->
                            overlayController.show(
                                type = MediaType.IMAGE,
                                urls = mediaUrls,
                                initialIndex = clickedIndex
                            )
                        }
                    )
                }
                MediaType.VIDEO -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        VideoPreviewPlayer(
                            videoUri = mediaUrls.first(),
                            modifier = Modifier.fillMaxSize(),
                            onFullscreenClick = {
                                overlayController.show(
                                    type = MediaType.VIDEO,
                                    urls = mediaUrls,
                                )
                            },
                            onAspectRatioAvailable = { newRatio ->
                                aspectRatio = newRatio
                            }
                        )
                    }
                }
                MediaType.PDF -> {
                    PdfPreviewContent(
                        pdfUri = mediaUrls.first().toUri(),
                        onFullscreenClick = {
                            overlayController.show(MediaType.PDF, mediaUrls)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PostImageGrid(
    images: List<String>,
    onImageClick: (Int) -> Unit
) {
    val count = images.size
    val gridHeight = 300.dp
    val spacing = 4.dp

    Box(modifier = Modifier.fillMaxWidth()) {
        when (count) {
            1 -> {
                var singleAspectRatio by remember { mutableFloatStateOf(1f) }
                PostGridImageItem(
                    uri = images[0],
                    onClick = { onImageClick(0) },
                    modifier = Modifier.fillMaxWidth().aspectRatio(singleAspectRatio),
                    contentScale = ContentScale.Fit,
                    onSuccess = { state ->
                        val width = state.painter.intrinsicSize.width
                        val height = state.painter.intrinsicSize.height
                        if (height != 0f) {
                            singleAspectRatio = width / height
                            if (singleAspectRatio < 1f) singleAspectRatio = 1f
                        }
                    }
                )
            }
            2 -> {
                Row(modifier = Modifier.fillMaxWidth().height(gridHeight), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                    PostGridImageItem(uri = images[0], onClick = { onImageClick(0) }, modifier = Modifier.weight(1f).fillMaxHeight())
                    PostGridImageItem(uri = images[1], onClick = { onImageClick(1) }, modifier = Modifier.weight(1f).fillMaxHeight())
                }
            }
            3 -> {
                Row(modifier = Modifier.fillMaxWidth().height(gridHeight), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                    PostGridImageItem(uri = images[0], onClick = { onImageClick(0) }, modifier = Modifier.weight(1f).fillMaxHeight())
                    Column(modifier = Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(spacing)) {
                        PostGridImageItem(uri = images[1], onClick = { onImageClick(1) }, modifier = Modifier.weight(1f).fillMaxWidth())
                        PostGridImageItem(uri = images[2], onClick = { onImageClick(2) }, modifier = Modifier.weight(1f).fillMaxWidth())
                    }
                }
            }
            else -> {
                Column(modifier = Modifier.fillMaxWidth().height(gridHeight), verticalArrangement = Arrangement.spacedBy(spacing)) {
                    Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                        PostGridImageItem(uri = images[0], onClick = { onImageClick(0) }, modifier = Modifier.weight(1f).fillMaxHeight())
                        PostGridImageItem(uri = images[1], onClick = { onImageClick(1) }, modifier = Modifier.weight(1f).fillMaxHeight())
                    }
                    Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                        PostGridImageItem(uri = images[2], onClick = { onImageClick(2) }, modifier = Modifier.weight(1f).fillMaxHeight())

                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            PostGridImageItem(uri = images[3], onClick = { onImageClick(3) }, modifier = Modifier.fillMaxSize())

                            if (count > 4) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.5f))
                                        .clickable { onImageClick(3) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "+${count - 4}",
                                        color = Color.White,
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PostGridImageItem(
    uri: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    onSuccess: ((coil3.compose.AsyncImagePainter.State.Success) -> Unit)? = null
) {
    Box(modifier = modifier.clip(RoundedCornerShape(8.dp))) {
        AsyncImage(
            model = uri.toUri(),
            contentDescription = "Post Image",
            onSuccess = onSuccess,
            modifier = Modifier.fillMaxSize().clickable(onClick = onClick),
            contentScale = contentScale
        )
    }
}
