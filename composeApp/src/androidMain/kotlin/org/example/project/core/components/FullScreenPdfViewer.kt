package org.example.project.core.components

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.example.project.R
import org.example.project.theme.IssueSpotTypography
import org.example.project.utils.pdfToBitmaps

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FullScreenPdfViewer(uri: Uri) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var pdfPages by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    val pagerState = rememberPagerState(pageCount = { pdfPages.size })

    // --- ZOOM STATE ---
    // We track scale and offset for the CURRENT page
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(uri) {
        pdfPages = pdfToBitmaps(context, uri)
    }

    // Reset zoom when page changes
    LaunchedEffect(pagerState.currentPage) {
        scale = 1f
        offset = Offset.Zero
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (pdfPages.isEmpty()) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.align(Alignment.Center))
        } else {
            // 1. The Carousel (Pager)
            // userScrollEnabled = false when zoomed in so you don't accidentally swipe to next page
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = scale == 1f,
                modifier = Modifier.fillMaxSize()
            ) { pageIndex ->

                // Content for one page
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                // A. Calculate Zoom
                                val newScale = (scale * zoom).coerceIn(1f, 4f)
                                scale = newScale

                                // B. Calculate Pan (only if zoomed)
                                if (scale > 1f) {
                                    // Calculate bounds (how far can we drag?)
                                    // Simple approximation: width * (scale - 1) / 2
                                    val maxX = (size.width * (scale - 1)) / 2
                                    val maxY = (size.height * (scale - 1)) / 2

                                    val newOffset = offset + pan
                                    offset = Offset(
                                        x = newOffset.x.coerceIn(-maxX, maxX),
                                        y = newOffset.y.coerceIn(-maxY, maxY)
                                    )
                                } else {
                                    offset = Offset.Zero
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = pdfPages[pageIndex].asImageBitmap(),
                        contentDescription = "Page $pageIndex",
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            // APPLY TRANSFORMATIONS HERE
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = offset.x
                                translationY = offset.y
                            },
                        contentScale = ContentScale.FillWidth
                    )
                }
            }

            // 2. Navigation Arrows & Indicator
            // ONLY SHOW CONTROLS IF NOT ZOOMED (scale == 1f)
            if (scale == 1f) {

                // ARROWS
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // LEFT ARROW
                    if (pagerState.currentPage > 0) {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                            },
                            modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_back_arrow),
                                contentDescription = "Previous Page",
                                tint = Color.White
                            )
                        }
                    } else {
                        Spacer(Modifier.size(48.dp))
                    }

                    // RIGHT ARROW
                    if (pagerState.currentPage < pdfPages.size - 1) {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            },
                            modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_next_arrow),
                                contentDescription = "Next Page",
                                tint = Color.White
                            )
                        }
                    } else {
                        Spacer(Modifier.size(48.dp))
                    }
                }

                // PAGE INDICATOR
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Page ${pagerState.currentPage + 1} of ${pdfPages.size}",
                        color = Color.White,
                        style = IssueSpotTypography.bodyMedium
                    )
                }
            }
        }
    }
}