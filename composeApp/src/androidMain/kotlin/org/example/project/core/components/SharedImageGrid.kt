package org.example.project.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import org.example.project.R

@Composable
fun SharedImageGrid(
    images: List<String>,
    onRemove: (String) -> Unit = {},
    onImageClick: (Int) -> Unit = {},
    isEditable: Boolean = false,
) {
    val count = images.size
    val gridHeight = 300.dp // Fixed height for multi-image grids to look uniform
    val spacing = 4.dp

    Box(modifier = Modifier.fillMaxWidth()) {
        when (count) {
            1 -> {
                // Single Image
                var singleAspectRatio by remember { mutableFloatStateOf(1f) }
                SharedGridImageItem(
                    uri = images[0],
                    onRemove = { onRemove(images[0]) },
                    onClick = { onImageClick(0) },
                    modifier = Modifier.fillMaxWidth().aspectRatio(singleAspectRatio),
                    contentScale = ContentScale.Fit,
                    showRemoveButton = false,
                    onSuccess = { state ->
                        val width = state.painter.intrinsicSize.width
                        val height = state.painter.intrinsicSize.height
                        if (height != 0f) {
                            singleAspectRatio = width / height
                            if (singleAspectRatio < 1f) singleAspectRatio = 1f
                        }
                    },
                )
            }
            2 -> {
                // Two Images - Side by side
                Row(modifier = Modifier.fillMaxWidth().height(gridHeight), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                    SharedGridImageItem(uri = images[0], onRemove = {
                        onRemove(images[0])
                    }, onClick = { onImageClick(0) }, modifier = Modifier.weight(1f).fillMaxHeight(), showRemoveButton = isEditable)
                    SharedGridImageItem(uri = images[1], onRemove = {
                        onRemove(images[1])
                    }, onClick = { onImageClick(1) }, modifier = Modifier.weight(1f).fillMaxHeight(), showRemoveButton = isEditable)
                }
            }
            3 -> {
                // Three Images - 1 Large Left, 2 Small Right vertically stacked
                Row(modifier = Modifier.fillMaxWidth().height(gridHeight), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                    SharedGridImageItem(uri = images[0], onRemove = {
                        onRemove(images[0])
                    }, onClick = { onImageClick(0) }, modifier = Modifier.weight(1f).fillMaxHeight(), showRemoveButton = isEditable)
                    Column(modifier = Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(spacing)) {
                        SharedGridImageItem(uri = images[1], onRemove = {
                            onRemove(images[1])
                        }, onClick = { onImageClick(1) }, modifier = Modifier.weight(1f).fillMaxWidth(), showRemoveButton = isEditable)
                        SharedGridImageItem(uri = images[2], onRemove = {
                            onRemove(images[2])
                        }, onClick = { onImageClick(2) }, modifier = Modifier.weight(1f).fillMaxWidth(), showRemoveButton = isEditable)
                    }
                }
            }
            else -> {
                // Four or More Images - 2x2 Grid with +N overlay on the 4th
                Column(modifier = Modifier.fillMaxWidth().height(gridHeight), verticalArrangement = Arrangement.spacedBy(spacing)) {
                    Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                        SharedGridImageItem(uri = images[0], onRemove = {
                            onRemove(images[0])
                        }, onClick = { onImageClick(0) }, modifier = Modifier.weight(1f).fillMaxHeight(), showRemoveButton = isEditable)
                        SharedGridImageItem(uri = images[1], onRemove = {
                            onRemove(images[1])
                        }, onClick = { onImageClick(1) }, modifier = Modifier.weight(1f).fillMaxHeight(), showRemoveButton = isEditable)
                    }
                    Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                        SharedGridImageItem(uri = images[2], onRemove = {
                            onRemove(images[2])
                        }, onClick = { onImageClick(2) }, modifier = Modifier.weight(1f).fillMaxHeight(), showRemoveButton = isEditable)

                        // 4th Image with Overlay
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            SharedGridImageItem(uri = images[3], onRemove = {
                                onRemove(images[3])
                            }, onClick = { onImageClick(3) }, modifier = Modifier.fillMaxSize(), showRemoveButton = isEditable)

                            if (count > 4) {
                                Box(
                                    modifier =
                                        Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.5f))
                                            .clickable { onImageClick(3) },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "+${count - 4}",
                                        color = Color.White,
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
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
fun SharedGridImageItem(
    uri: String,
    onRemove: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    showRemoveButton: Boolean = false,
    onSuccess: ((coil3.compose.AsyncImagePainter.State.Success) -> Unit)? = null,
) {
    Box(modifier = modifier.clip(RoundedCornerShape(8.dp))) {
        AsyncImage(
            model = uri.toUri(),
            contentDescription = "Selected Image",
            onSuccess = onSuccess,
            modifier = Modifier.fillMaxSize().clickable(onClick = onClick),
            contentScale = contentScale,
            error = painterResource(R.drawable.img_post_placeholder),
            fallback = painterResource(R.drawable.img_post_placeholder),
        )
        if (showRemoveButton) {
            IconButton(
                onClick = onRemove,
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .size(24.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = "Remove",
                    modifier = Modifier.size(14.dp),
                    tint = Color.White,
                )
            }
        }
    }
}
