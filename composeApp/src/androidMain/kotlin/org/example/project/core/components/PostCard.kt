package org.example.project.core.components

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
import coil3.compose.AsyncImagePainter
import org.example.project.core.model.home.MediaType
import org.example.project.core.model.home.Post
import org.example.project.core.model.home.PostLevel
import org.example.project.theme.IssueSpotTheme
import org.example.project.utils.getColor

@Composable
fun PostCard(
    post: Post,
    isLiked: Boolean,
    likesCount: Int,
    commentsCount: Int,
    isReported: Boolean,
    modifier: Modifier = Modifier,
    canDelete: Boolean = false,
    canReport : Boolean = true,
    onLikeClick: () -> Unit,
    onCommentIconClick: () -> Unit,
    onShareClick: () -> Unit,
    onReportClick: (String) -> Unit,
    onDeleteClick: () -> Unit = {},
    onPostClick: () -> Unit = {},
    onCollapseClick: () -> Unit = {},
    isDetailMode: Boolean = false
) {
    val spacing = IssueSpotTheme.spacing
    val shapes = MaterialTheme.shapes
    var showReportDialog by rememberSaveable { mutableStateOf(false) }
    var hasOverflow by remember { mutableStateOf(false) }

    println("[KMP_PAGING]\nPostCard BODY\npostId: ${post.id}\nlikes: $likesCount\ncomments: $commentsCount")

    androidx.compose.runtime.DisposableEffect(post.id) {
        println("[KMP_PAGING]\nPostCard APPEAR\npostId: ${post.id}")
        onDispose {
            println("[KMP_PAGING]\nPostCard DISAPPEAR\npostId: ${post.id}")
        }
    }

    Box {
        Card(
            modifier = modifier
                .then(
                    if (isDetailMode) {
                        Modifier.fillMaxSize()
                    } else {
                        Modifier.fillMaxWidth()
                    }
                ),
            colors = CardDefaults.cardColors(containerColor = IssueSpotColors.CardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            shape = shapes.medium
        ) {
            Box(modifier = Modifier.fillMaxWidth().then(if (isDetailMode) Modifier.fillMaxHeight() else Modifier)) {
                Column(
                    modifier = Modifier
                        .padding(spacing.medium)
                        .then(if (isDetailMode) Modifier.fillMaxHeight() else Modifier)
                ) {
                    Column(
                        modifier = Modifier
                            .then(if (isDetailMode) Modifier.weight(1f) else Modifier)
                            .clickable(enabled = !isDetailMode) {
                                onPostClick()
                            }
                    ) {
                        Spacer(modifier = Modifier.height(spacing.small))

                        val locationParts = listOfNotNull(post.locality, post.district, post.state, post.country).filter { it.isNotBlank() }
                        val locationString = if (locationParts.isNotEmpty()) locationParts.joinToString(", ") else "Unknown Location"

                        PostHeader(
                            userName = post.userName,
                            userImageUrl = post.userUrl,
                            timeAgo = post.timeAgo,
                            postLevel = post.postLevel,
                            location = locationString,
                            isDetailMode = isDetailMode,
                            onCollapseClick = onCollapseClick
                        )

                        Spacer(modifier = Modifier.height(spacing.medium))

                        Text(
                            text = post.postText,
                            style = IssueSpotTypography.bodyLarge,
                            maxLines = if (isDetailMode) Int.MAX_VALUE else 4,
                            overflow = TextOverflow.Ellipsis,
                            onTextLayout = {
                                hasOverflow = it.hasVisualOverflow
                            }
                        )

                        if (!isDetailMode && hasOverflow) {
                            Text(
                                text = "More",
                                style = IssueSpotTypography.bodyMedium,
                                color = IssueSpotColors.Primary,
                                modifier = Modifier
                                    .padding(top = spacing.extraSmall)
                                    .clickable {
                                        onPostClick()
                                    }
                            )
                        }

                        Spacer(modifier = Modifier.height(spacing.smallMedium))

                        if (!post.mediaUrls.isNullOrEmpty()) {
                            PostMediaPreview(post)
                        }
                    }

                    Spacer(modifier = Modifier.height(spacing.smallMedium))
                    Row(
                        modifier = Modifier.padding(horizontal = spacing.small, vertical = spacing.extraSmall),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(
                                id = if (isLiked) R.drawable.ic_thumbs_filled else R.drawable.ic_thumbs
                            ),
                            contentDescription = "Like",
                            modifier = Modifier
                                .clickable { onLikeClick() }
                                .size(20.dp)
                                .padding(end = 2.dp),
                            tint = if (isLiked)
                                Color(0xFF0A66C2)
                            else
                                IssueSpotColors.OnSurfaceVariant
                        )
                        Text(
                            text = likesCount.toString(),
                            style = IssueSpotTypography.bodyLarge,
                        )

                        Spacer(modifier = Modifier.width(spacing.medium))

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

                        if(canReport) {

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
                        }

                        Spacer(modifier = Modifier.width(spacing.medium))

                        Icon(
                            painter = painterResource(R.drawable.ic_share),
                            contentDescription = "Share",
                            modifier = Modifier
                                .clickable { onShareClick() }
                                .size(20.dp)
                                .padding(end = 2.dp),
                            tint = IssueSpotColors.OnSurfaceVariant
                        )

                        if (canDelete) {
                            Spacer(modifier = Modifier.width(spacing.medium))
                            Icon(
                                painter = painterResource(R.drawable.ic_delete),
                                contentDescription = "Delete",
                                modifier = Modifier
                                    .clickable { onDeleteClick() }
                                    .size(20.dp)
                                    .padding(end = 2.dp),
                                tint = IssueSpotColors.Error
                            )
                        }
                    }
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

@Composable
fun PostHeader(
    userName: String,
    userImageUrl: String? = null,
    timeAgo: String? = null,
    postLevel: PostLevel,
    location: String,
    modifier: Modifier = Modifier,
    isDetailMode: Boolean = false,
    onCollapseClick: () -> Unit = {}
) {
    val spacing = IssueSpotTheme.spacing
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {

        if (isDetailMode) {
            IconButton(
                onClick = onCollapseClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = "Close",
                    modifier = Modifier.size(20.dp),
                    tint = IssueSpotColors.OnSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(spacing.extraSmall))
        }

        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(IssueSpotColors.SurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (!userImageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = userImageUrl.toUri(),
                    contentDescription = "$userName avatar",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                    error = painterResource(R.drawable.ic_user_avatar),
                    fallback = painterResource(R.drawable.ic_user_avatar)
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.ic_user_avatar),
                    contentDescription = null,
                    tint = IssueSpotColors.OnSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(spacing.smallMedium))

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {

                Text(
                    text = userName,
                    style = IssueSpotTypography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(spacing.small))

                PostLevelChip(postLevel)
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = location,
                style = IssueSpotTypography.bodySmall,
                color = IssueSpotColors.OnSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            if (!timeAgo.isNullOrBlank()) {

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = timeAgo,
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
    val spacing = IssueSpotTheme.spacing
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
            modifier = Modifier.padding(horizontal = spacing.smallMedium, vertical = spacing.extraSmall),
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
    userId = "user1",
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
    val mediaUrls = post.mediaUrls ?: return

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
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
                            onAspectRatioAvailable = { _ ->
                                // Ignored to keep layout height stable
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
                PostGridImageItem(
                    uri = images[0],
                    onClick = { onImageClick(0) },
                    modifier = Modifier.fillMaxWidth().height(gridHeight),
                    contentScale = ContentScale.Crop
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
    onSuccess: ((AsyncImagePainter.State.Success) -> Unit)? = null
) {
    Box(modifier = modifier.clip(RoundedCornerShape(8.dp))) {
        AsyncImage(
            model = uri.toUri(),
            contentDescription = "Post Image",
            onSuccess = onSuccess,
            modifier = Modifier.fillMaxSize().clickable(onClick = onClick),
            contentScale = contentScale,
            error = painterResource(R.drawable.img_post_placeholder),
            fallback = painterResource(R.drawable.img_post_placeholder)
        )
    }
}
