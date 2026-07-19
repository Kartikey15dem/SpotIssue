package org.example.project.core.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import org.example.project.R
import org.example.project.theme.IssueSpotTypography

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FullScreenImageViewer(
    imageUrls: List<String>,
    initialPage: Int = 0,
) {
    val scope = rememberCoroutineScope()
    val pagerState =
        rememberPagerState(
            initialPage = initialPage,
            pageCount = { imageUrls.size },
        )

    // --- ZOOM STATE ---
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Reset zoom when page changes
    LaunchedEffect(pagerState.currentPage) {
        scale = 1f
        offset = Offset.Zero
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // 1. The Carousel (Pager)
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = scale == 1f, // Lock scrolling if zoomed in
            modifier = Modifier.fillMaxSize(),
        ) { pageIndex ->

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val newScale = (scale * zoom).coerceIn(1f, 4f)
                                scale = newScale

                                if (scale > 1f) {
                                    val maxX = (size.width * (scale - 1)) / 2
                                    val maxY = (size.height * (scale - 1)) / 2
                                    val newOffset = offset + pan
                                    offset =
                                        Offset(
                                            x = newOffset.x.coerceIn(-maxX, maxX),
                                            y = newOffset.y.coerceIn(-maxY, maxY),
                                        )
                                } else {
                                    offset = Offset.Zero
                                }
                            }
                        },
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = imageUrls[pageIndex],
                    contentDescription = "Image ${pageIndex + 1}",
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            // APPLY TRANSFORMATIONS HERE
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = offset.x
                                translationY = offset.y
                            },
                    contentScale = ContentScale.Fit,
                    error = painterResource(R.drawable.img_post_placeholder),
                    fallback = painterResource(R.drawable.img_post_placeholder),
                )
            }
        }

        // 2. Navigation Arrows & Indicator
        if (scale == 1f && imageUrls.size > 1) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                        .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // LEFT ARROW
                if (pagerState.currentPage > 0) {
                    IconButton(
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                        },
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape),
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back_arrow),
                            contentDescription = "Previous",
                            tint = Color.White,
                        )
                    }
                } else {
                    Spacer(Modifier.size(48.dp))
                }

                // RIGHT ARROW
                if (pagerState.currentPage < imageUrls.size - 1) {
                    IconButton(
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        },
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape),
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_next_arrow),
                            contentDescription = "Next",
                            tint = Color.White,
                        )
                    }
                } else {
                    Spacer(Modifier.size(48.dp))
                }
            }

            // PAGE INDICATOR
            Box(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    text = "${pagerState.currentPage + 1} / ${imageUrls.size}",
                    color = Color.White,
                    style = IssueSpotTypography.bodyMedium,
                )
            }
        }
    }
}
